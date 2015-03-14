package controllers

import java.net.InetAddress
import java.util.UUID

import com.cave.metrics.data.{DataSink, Metric}
import org.joda.time.DateTime
import play.api.mvc.{Action, Request, Result, WrappedRequest}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object MeteredActionStatus extends Enumeration {
  type MeteredActionStatus = Value

  val Info, Success, Redirect, ClientError, ServerError, Unknown = Value

  def getStatus(code: Int): MeteredActionStatus =
    if (code < 200) Info
    else if (code < 300) Success
    else if (code < 400) Redirect
    else if (code < 500) ClientError
    else if (code < 600) ServerError
    else Unknown
}

case class Counter(value: Int, extraTags: Map[String, String] = Map.empty[String, String])
case class Timer(time: Double, extraTags: Map[String, String] = Map.empty[String, String])

case class MeteredWrappedRequest[A](requestId: String, request: Request[A]) extends WrappedRequest[A](request) {
  val counters = mutable.HashMap.empty[String, Counter]
  val timers = mutable.HashMap.empty[String, Timer]
}

object Stats {
  def addCounter[A](name: String, value: Int, extraTags: Map[String, String] = Map.empty[String, String])
                   (implicit request: Request[A]): Unit =
    request match {
      case wr: MeteredWrappedRequest[A] =>
        wr.counters += name -> Counter(value, extraTags)

      case _ => // cannot do much about it
    }

  def addTimer[A](name: String, value: Double, extraTags: Map[String, String] = Map.empty[String, String])
                 (implicit request: Request[A]): Unit =
    request match {
      case wr: MeteredWrappedRequest[A] =>
        wr.timers += name -> Timer(value, extraTags)

      case _ => // cannot do much about it
    }
}

case class MeteredAction[A](service: String, operation: String)
                           (action: Action[A])
                           (implicit metricsEnabled: Boolean, dataSink: DataSink, ec: ExecutionContext) extends Action[A] {

  import controllers.MeteredActionStatus._

  val hostname = InetAddress.getLocalHost.getCanonicalHostName

  private def createMetrics(timestamp: Long, request: MeteredWrappedRequest[A], requestTags: Map[String, String]): Seq[Metric] = {
    val counters = request.counters map { case (name, counter) =>
      Metric(name, timestamp, counter.value, requestTags ++ counter.extraTags)
    }

    val timers = request.timers map { case (name, timer) =>
      Metric(name, timestamp, timer.time, requestTags ++ timer.extraTags)
    }

    counters.toSeq ++ timers.toSeq
  }

  override val parser = action.parser

  override def apply(request: Request[A]): Future[Result] = {
    if (metricsEnabled) {
      val startNanos = System.nanoTime()
      val meteredRequest = MeteredWrappedRequest(UUID.randomUUID().toString, request)

      val futureResult = action(meteredRequest)

      futureResult.onComplete {
        result: Try[Result] =>
          val timestamp = new DateTime().getMillis / 1000
          val responseTime = (System.nanoTime() - startNanos) / 1000000
          val httpStatus = result map (r => MeteredActionStatus.getStatus(r.header.status)) getOrElse Unknown

          val requestTags: Map[String, String] = Map(
            "service" -> service,
            "host" -> hostname,
            "operation" -> operation,
            "status" -> httpStatus.toString,
            Metric.Organization -> Metric.Internal
          )

          val responseTimeMetric = Metric("response-time", timestamp, responseTime, requestTags)
          val requestMetrics = createMetrics(timestamp, meteredRequest, requestTags)
          dataSink.sendMetrics(responseTimeMetric +: requestMetrics)
      }

      futureResult
    } else action(request)
  }
}
