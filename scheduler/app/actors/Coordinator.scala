package actors

import java.util.concurrent.Executor

import actors.Scheduler.NotificationUrlChange
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.cave.metrics.data._
import init.AwsWrapper
import init.AwsWrapper.WorkItem
import org.apache.commons.logging.LogFactory
import play.api.libs.json._

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Coordinator {
  // Key is AlertId, Value is Alert Scheduler Actor
  type SchedulesMap = mutable.Map[String, ActorRef]

  // Key is OrgName, value is a list of Alert IDs for this Org
  type SchedulesCache = mutable.Map[String, List[String]]

  object CheckQueue
  object StatusRequest
  case class StatusResponse(cache: CoordinatorCache, schedulers: SchedulesMap)

  implicit val cacheWrites = new Writes[CoordinatorCache] {
    def writes(obj: CoordinatorCache): JsValue = Json.arr(
      obj.schedulesByOrganization.map { case (orgName, alertList) =>
        Json.obj(
          "organization" -> orgName,
          "alerts" -> Json.toJson(alertList)
        )
      }
    )
  }

  implicit val mapWrites = new Writes[SchedulesMap] {
    def writes(obj: SchedulesMap): JsValue = Json.toJson(obj.keysIterator.toList)
  }

  implicit val responseWrites = new Writes[StatusResponse] {
    def writes(obj: StatusResponse): JsValue = Json.obj(
      "cache" -> Json.toJson(obj.cache),
      "schedulers" -> Json.toJson(obj.schedulers)
    )
  }
}

import actors.Coordinator._

trait CoordinatorCacheListener {

  def createScheduler(schedule: Schedule)
  def stopScheduler(scheduleId: String)
  def deleteMessage(receiptHandle: String)
  def notifyUrlChange(scheduleId: String, newUrl: String)
}

class CoordinatorCache(cacheManager: CacheDataManager, listener: CoordinatorCacheListener) {

  private final val Log = LogFactory.getLog(this.getClass)

  val schedulesByOrganization: SchedulesCache = {
    val scheduleMap = cacheManager.getEnabledAlerts().getOrElse(
      sys.error("Failed to retrieve alerts from data source. Quitting..."))

    mutable.Map.empty[String, List[String]] ++= scheduleMap map {
      case (orgName, orgSchedules) =>
        orgSchedules foreach listener.createScheduler
        orgName -> orgSchedules.map(_.alert.id.get)
    }
  }
  Log.warn("Initial data: " + schedulesByOrganization)

  private[actors] def updateCache(message: WorkItem) = {

    message.update.entityType match {
      case Entity.Organization =>
        Log.warn("Entity: Org")
        updateOrganization(message.update)

      case Entity.Alert =>
        Log.warn("Entity: Alert")
        updateSchedule(message.update)

      case x =>
        Log.warn(s"Ignoring update of unsupported entity type $x")
    }

    listener.deleteMessage(message.receiptHandle)
  }

  private[actors] def updateOrganization(update: Update): Unit = {
    val orgName = update.id

    update.operation match {
      case Operation.Create =>
        schedulesByOrganization.get(orgName) match {
          case None =>
            schedulesByOrganization += orgName -> List.empty[String]

          case Some(_) =>
            Log.warn(s"Unexpected create received for known Organization $orgName.")
        }


      case Operation.Update =>
        schedulesByOrganization.get(orgName) match {
          case Some(schedules) =>
            schedules foreach (id => listener.notifyUrlChange(id, update.extra))

          case None =>
            Log.warn(s"Unexpected update received for unknown Organization $orgName.")
        }

      case Operation.Delete =>
        schedulesByOrganization.remove(orgName) match {
          case Some(schedules) =>
            schedules foreach listener.stopScheduler

          case None =>
            Log.warn(s"Unexpected delete received for unknown Organization $orgName.")
        }
    }
  }

  private[actors] def updateSchedule(update: Update): Unit =
    update.operation match {
      case Operation.Create =>
        getSchedule(update.extra) foreach { schedule =>
            if (schedule.alert.enabled) {
              schedulesByOrganization.get(schedule.orgName) foreach { schedules =>
                schedules.find(_ == update.id) match {
                  case None =>
                    listener.createScheduler(schedule)
                    schedulesByOrganization.update(schedule.orgName, update.id :: schedules)

                  case Some(_) =>
                    Log.warn(s"Unexpected create received for existing alert with id ${update.id}.")
                }
              }
            } else {
              Log.debug("Alert is disabled. Not creating a scheduler for it.")
            }
        }

      case Operation.Update =>
        getSchedule(update.extra) foreach { schedule =>
          schedulesByOrganization.get(schedule.orgName) foreach { schedules =>
            schedules.filter(_ == update.id).foreach(listener.stopScheduler)

            if (schedule.alert.enabled) {
              // if we just enabled an alert, we need to add it to the map
              schedulesByOrganization.update(schedule.orgName, update.id :: schedules.filterNot(_ == update.id))

              listener.createScheduler(schedule)
            } else {
              // if we just disabled an alert, we need to remove it from the map
              schedulesByOrganization.update(schedule.orgName, schedules.filterNot(_ == update.id))

              Log.debug("Alert is disabled. Not creating a new scheduler for it.")
            }
          }
        }

      case Operation.Delete =>
        val orgName = update.extra
        schedulesByOrganization.get(orgName) foreach { schedules =>
          schedules.find(_ == update.id) match {
            case Some(_) =>
              schedulesByOrganization.update(orgName, schedules.filterNot(_ == update.id))
              listener.stopScheduler(update.id)

            case None =>
              Log.warn(s"Unexpected delete received for unknown alert with id ${update.id}.")
          }
        }
    }

  private[actors] def getSchedule(extra: String): Option[Schedule] =
    try {
      Some(Json.parse(extra).as[Schedule])
    } catch {
      case e: Exception =>
        Log.warn("Received a bad extra: " + extra + ". Expected a Schedule entity in JSON format.")
        None
    }

}

class Coordinator(awsWrapper: AwsWrapper, cacheManager: CacheDataManager) extends Actor with ActorLogging with CoordinatorCacheListener {

  private[actors] def WorkPeriod = 10.seconds

  implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]
  private[actors] val schedulers: SchedulesMap = mutable.Map.empty[String, ActorRef]
  private[actors] val cache = new CoordinatorCache(cacheManager, this)

  private val queueCheckSchedule = context.system.scheduler.schedule(0.minutes, WorkPeriod, self, Coordinator.CheckQueue)

  override def receive = {
    case Coordinator.CheckQueue =>
      awsWrapper.receiveMessages() foreach { messageList =>
        log.warning("Messages: " + messageList)
        messageList foreach cache.updateCache
      }

    case Coordinator.StatusRequest =>
      sender ! StatusResponse(cache, schedulers)
  }

  override def postStop(): Unit = queueCheckSchedule.cancel()

  override def createScheduler(schedule: Schedule) =
    schedulers += schedule.alert.id.get -> context.actorOf(Props(new Scheduler(schedule, awsWrapper)))

  override def stopScheduler(scheduleId: String) =
    schedulers.remove(scheduleId) foreach(_ ! Scheduler.Die)

  override def deleteMessage(receiptHandle: String) =
    awsWrapper.deleteMessage(receiptHandle)

  override def notifyUrlChange(scheduleId: String, newUrl: String) =
    schedulers.get(scheduleId) foreach(_ ! NotificationUrlChange(newUrl))
}
