package com.cave.metrics.data.evaluator

import com.cave.metrics.data.ExponentialBackOff
import com.cave.metrics.data.influxdb.InfluxClientFactory
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

class DataFetcher(clientFactory: InfluxClientFactory) extends ExponentialBackOff {

  // parameters for Exponential BackOff
  protected val MaxBackOffTimeInMillis = 1000L
  protected val ShouldLogErrors = true
  def maxTries: Int = 3

  def fetchData(clusterName: Option[String], databaseName: String, metricName: String, metricTags: Map[String, String],
                repeats: Int, delay: FiniteDuration, end: DateTime)(implicit ec: ExecutionContext): Future[Option[List[Double]]] =
    try {
      val delayedEnd = end.minusMinutes(delay.toMinutes.toInt)
      val (client, context) = clientFactory.getClient(clusterName)

      retryUpTo(maxTries) {
        client.getMetricData(
          database = databaseName,
          metric = metricName,
          tags = metricTags,
          start = None,
          end = Some(delayedEnd),
          limit = Some(repeats))(context)
      } map {
        case scala.util.Success(data) => data.map(_.metrics.map(_.value).toList)

        case scala.util.Failure(t) => sys.error(t.getMessage)
      }
    } catch {
      case e: RuntimeException =>
        Future.failed(e)
    }

  def fetchData(clusterName: Option[String], databaseName: String, metricName: String, metricTags: Map[String, String],
                duration: FiniteDuration, end: DateTime)(implicit ec: ExecutionContext): Future[Option[List[Double]]] =
    try {
      val (client, context) = clientFactory.getClient(clusterName)
      retryUpTo(maxTries) {
        client.getMetricData(
          database = databaseName,
          metric = metricName,
          tags = metricTags,
          start = Some(end.minusSeconds(duration.toSeconds.toInt)),
          end = Some(end),
          limit = None)(context)
      } map {
        case scala.util.Success(data) => data.map(_.metrics.map(_.value).toList)

        case scala.util.Failure(t) => sys.error(t.getMessage)
      }
    } catch {
      case e: RuntimeException =>
        Future.failed(e)
    }

  def fetchData(clusterName: Option[String], databaseName: String, aggregator: String, period: FiniteDuration,
                metric: String, tags: Map[String, String], repeats: Int, delay: FiniteDuration, end: DateTime)
               (implicit ec: ExecutionContext) = {
    try {
      val delayedEnd = end.minusMinutes(delay.toMinutes.toInt)
      val (client, context) = clientFactory.getClient(clusterName)
      retryUpTo(maxTries) {
        client.getAggregatedData(
          databaseName,
          aggregator, period,
          metric, tags,
          start = None,
          end = Some(delayedEnd),
          limit = Some(repeats)
        )(context)
      } map {
        case scala.util.Success(data) => data.map(_.metrics.map(_.value).toList)
        case scala.util.Failure(t) => sys.error(t.getMessage)
      }
    } catch {
      case NonFatal(e) =>
        Future.failed(e)
    }
  }
}
