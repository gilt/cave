package worker

import java.util.concurrent.Executor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.cave.metrics.data.Metric
import init.AwsWrapper
import init.AwsWrapper.WorkItem
import play.api.libs.json.{JsValue, Json, Writes}
import worker.Coordinator.StatusResponse

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Coordinator {

  object DoWork

  object StatusRequest

  case class StatusResponse(currentlyActive: Long, aborted: Long, totalProcessed: Long, noOfAlarmsTriggered: Long)

  implicit val responseWrites = new Writes[StatusResponse] {
    def writes(obj: StatusResponse): JsValue = Json.obj(
      "totalChecksProcessed" -> Json.toJson(obj.totalProcessed),
      "abortedChecks" -> Json.toJson(obj.aborted),
      "activeCheckers" -> Json.toJson(obj.currentlyActive),
      "noOfAlarmsTriggered" -> Json.toJson(obj.noOfAlarmsTriggered)
    )
  }

}

class Coordinator(awsWrapper: AwsWrapper, shouldSendHistory: Boolean = true) extends Actor with ActorLogging {

  implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]

  private val workSchedule = context.system.scheduler.schedule(0.minutes, 10.seconds, self, Coordinator.DoWork)
  private[worker] val checkers = mutable.Map.empty[ActorRef, WorkItem]
  private var noOfAlarmsTriggered: Long = 0
  private var noOfChecksDone: Long = 0
  private var noOfChecksAborted: Long = 0

  override def receive = {

    case Coordinator.DoWork =>
      val messages = awsWrapper.receiveMessages()
      messages map { list =>
        log.info("Scheduler message to check the queue for work items: {} items found", list.length)
        list foreach createChildCheckers
      }

    case Checker.Done(alarm) =>
      log.info("Received message that check was done, alarm {}", alarm)
      checkers.remove(sender()).foreach { item =>
        if (shouldSendHistory) {
          alarm match {
            case Success(result) =>
              awsWrapper.dataSink.sendMetric(createMetric(item, Some(result), None))

            case Failure(error) =>
              awsWrapper.dataSink.sendMetric(createMetric(item, None, Some(error.getMessage)))
          }

          awsWrapper.dataSink.sendMetric(createKpiMetric(item))
        }

        alarm map { result =>
          if (result) {
            log.info("Creating a notifier actor to send alarm notification.")
            createNotifier(item)
            noOfAlarmsTriggered += 1
          }
        }
        noOfChecksDone += 1
        awsWrapper.deleteMessage(item.receiptHandle)
      }

    case Checker.Aborted(reason) =>
      log.info("Received message that check was aborted: {}", reason)
      checkers.remove(sender()) foreach { item =>
        if (shouldSendHistory) {
          awsWrapper.dataSink.sendMetric(createMetric(item, None, Some(Metric.InternalError)))
          awsWrapper.dataSink.sendMetric(createKpiMetric(item))
        }

        noOfChecksAborted += 1
        awsWrapper.deleteMessage(item.receiptHandle)
      }

    case Notifier.Done(result) =>
      log.info("Received message that notification was delivered: {}", result)

    case Coordinator.StatusRequest =>
      sender ! StatusResponse(checkers.size, noOfChecksAborted, noOfChecksDone, noOfAlarmsTriggered)
  }

  def createNotifier(item: WorkItem): Unit = context.actorOf(Props(new Notifier(item.check)))
  override def postStop(): Unit = workSchedule.cancel()

  def createChildCheckers(workItem: WorkItem) = {
    log.info("Creating a checker for work item: {}", workItem)
    checkers += context.actorOf(Props(new Checker(workItem.check))) -> workItem
  }

  def createMetric(item: WorkItem, alarm: Option[Boolean], error: Option[String]): Metric = {
    new Metric(
     "alertsHistory",
     item.check.timestamp.getMillis / 1000,
     alarm match {
       case None => -1
       case Some(true) => 1
       case Some(false) => 0
     },
     Seq(
       item.check.schedule.alert.id map ("alert" -> _),
       Option(Metric.Organization -> item.check.schedule.databaseName),
       item.check.schedule.clusterName map (Metric.Cluster -> _),
       error map (Metric.Error -> _)
     ).flatten.toMap
  )}

  def createKpiMetric(item: WorkItem): Metric = new Metric(
    "alertsActive",
    item.check.timestamp.getMillis / 1000,
    1,
    Seq(
      item.check.schedule.alert.id map ("alert" -> _),
      Option("organization" -> item.check.schedule.orgName),
      item.check.schedule.teamName map ("team" -> _),
      Option(Metric.Organization -> "cave-kpi"),
      item.check.schedule.clusterName map (Metric.Cluster -> _)
    ).flatten.toMap
  )
}
