package worker

import java.util.concurrent.Executor

import akka.actor.{Actor, ActorLogging, Status}
import akka.pattern.pipe
import com.cave.metrics.data.Check
import init.Init
import worker.web.NotificationSender

import scala.concurrent._

object Notifier {
  case class Done(result: Boolean)
}

class Notifier(notification: Check) extends Actor with ActorLogging {

  def client: NotificationSender = Init.sender

  implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]

  client send notification pipeTo self

  override def receive = {

    case success: Boolean =>
      log.info("Posted notification, success = {}", success)
      context.parent ! Notifier.Done(success)
      stop()

    case _: Status.Failure =>
      log.info("Failed to send notification.")
      stop()
  }

  def stop(): Unit = {
    context.stop(self)
  }
}
