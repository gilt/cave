package worker

import java.util.concurrent.Executor

import akka.actor.{Actor, ActorLogging, Status}
import akka.pattern.pipe
import com.cave.metrics.data._
import com.cave.metrics.data.evaluator.{CheckEvaluator, DataFetcher}
import init.Init

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Checker {
  type Result = Try[Boolean]

  case class Done(alarm: Result)
  case class Aborted(reason: String)
}

class Checker(check: Check) extends Actor with ActorLogging {

  implicit val exec = context.dispatcher.asInstanceOf[Executor with ExecutionContext]
  val evaluator = new CheckEvaluator(check)
  def fetcher = new DataFetcher(Init.influxClientFactory)

  this run check pipeTo self

  def receive = {
    case alarm: Checker.Result =>
      context.parent ! Checker.Done(alarm)
      stop()

    case x: Status.Failure =>
      context.parent ! Checker.Aborted(x.cause.getMessage)
      stop()
  }

  def stop(): Unit = {
    context stop self
  }

  private[worker] def run(check: Check)(implicit ec: ExecutionContext): Future[Try[Boolean]] = {
    val result = evaluator.evaluate(fetcher)
    result map { v =>
      log.warning("Result of evaluation: " + v)
    }
    result
  }
}
