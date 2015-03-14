package actors

import java.util.concurrent.{Executor, TimeUnit}

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import com.cave.metrics.data.evaluator.AlertParser
import com.cave.metrics.data.{Check, Schedule}
import init.{AwsWrapper, Init}
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{Minutes, LocalTime, DateTime, DateTimeZone}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Scheduler {
  object DoWork
  object Die
  case class NotificationUrlChange(newUrl: String)
}
class Scheduler(schedule: Schedule, awsWrapper: AwsWrapper) extends Actor with ActorLogging with AlertParser {

  private[actors] def leader = Init.leader
  var notificationUrl: String = schedule.notificationUrl
  implicit val timeout = Timeout(2, TimeUnit.SECONDS)

  val (waitTime, period) = getSchedule(schedule.alert.period)

  val Formatter = ISODateTimeFormat.dateTimeNoMillis()

  implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]
  private val queueCheckSchedule = context.system.scheduler.schedule(waitTime, period, self, Scheduler.DoWork)

  override def receive = {
    case Scheduler.DoWork =>
      leader ? Leadership.IsLeader onComplete {
        case scala.util.Success(imLeader: Boolean) =>
          if (imLeader) {
            awsWrapper.sendMessage(Check(Schedule(schedule.orgName, schedule.teamName, schedule.clusterName, notificationUrl, schedule.alert), now()))
          }

        case scala.util.Success(e) =>
          log.error("Unexpected result returned by the leader actor: " + e)

        case scala.util.Failure(t) =>
          log.error("Failed to query the leader actor, error was " + t)
      }


    case Scheduler.NotificationUrlChange(url) =>
      log.debug(s"Updating the notification URL, from $notificationUrl to $url.")
      notificationUrl = url

    case Scheduler.Die =>
      context stop self
  }

  override def postStop(): Unit = queueCheckSchedule.cancel()

  /**
   * Can be overridden in tests
   *
   * @return current date time in UTC
   */
  private[actors] def now(): DateTime = new DateTime(DateTimeZone.UTC)
  private[actors] def nowLocal(): LocalTime = new LocalTime()

  /**
   * Parse the alert period and compute how long until the next check and the check interval
   *
   * @param alertPeriod  a string that can be parsed as a duration or a daily schedule
   * @return             a wait duration and the check interval
   */
  private[actors] def getSchedule(alertPeriod: String): (FiniteDuration, FiniteDuration) =
    parseAll(duration, alertPeriod) match {
      case Success(p, _) => (0.minutes, p)

      case NoSuccess(_, message) =>
        parseAll(daily, alertPeriod) match {
          case Success(time, _) => (getWait(nowLocal(), time), 1.day)

          case NoSuccess(_, message2) =>
            sys.error(s"Unexpected alert period $alertPeriod. Not a duration ($message) and not a daily scheduler ($message2).")
        }
    }

  private[actors] def getWait(now: LocalTime, until: LocalTime): FiniteDuration = {
    val wait = Minutes.minutesBetween(now, until).getMinutes
    val minutes = if (wait < 0) 1440 + wait else wait
    minutes.minutes
  }
}
