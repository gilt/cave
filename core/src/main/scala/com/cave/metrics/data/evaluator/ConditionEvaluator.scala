package com.cave.metrics.data.evaluator

import com.cave.metrics.data.influxdb.InfluxClientFactory
import com.cave.metrics.data.{MetricCheckRequest, MetricData, MetricDataBulk}
import org.joda.time.{DateTime, Period}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class ConditionEvaluator(clusterName: Option[String], databaseName: String, request: MetricCheckRequest) extends AbstractEvaluator(request.condition) {

  def evaluate(clientFactory: InfluxClientFactory)(implicit ec: ExecutionContext): Future[Option[MetricDataBulk]] = {

    val fetcher = new DataFetcher(clientFactory)
    val step = Period.minutes(request.interval)
    val dateRange = Iterator.iterate(request.start)(_.plus(step)).takeWhile(!_.isAfter(request.end))

    def evaluateOnce(rangeStartDate: DateTime): Future[Option[MetricData]] = {
      val rangeEndDate = rangeStartDate.plus(step).minusSeconds(1)

      evaluateRange(clusterName, databaseName, rangeEndDate)(fetcher, ec) map {
        case util.Success(value) =>
          Some(MetricData(rangeStartDate, if (value) 1.0 else 0.0))
        case _ => None
      }
    }

    // If the result of one evaluation is None, it means the metric does not exist!
    // In that case, there's no point in evaluating any other dates in the range.
    evaluateOnce(dateRange.next()) flatMap {
      case Some(value) =>
        val results = dateRange map evaluateOnce
        Future.sequence(results) map(seq => Some(MetricDataBulk(value +: seq.flatten.toSeq)))

      case None =>
        Future.successful(None)
    }
  }

  override def getData(clusterName: Option[String], databaseName: String, metricName: String,
                       metricTags: Map[String, String], repeats: Int, delay: FiniteDuration, end: DateTime)
                      (implicit fetcher: DataFetcher, ec: ExecutionContext): Future[Option[List[Double]]] =
    fetcher.fetchData(clusterName, databaseName, metricName, metricTags, repeats, delay, end)(ec)

  override def getData(clusterName: Option[String], databaseName: String, metricName: String,
                       metricTags: Map[String, String], duration: FiniteDuration, end: DateTime)
                      (implicit fetcher: DataFetcher, ec: ExecutionContext): Future[Option[List[Double]]] =
    fetcher.fetchData(clusterName, databaseName, metricName, metricTags, duration, end)(ec)

  override def getData(clusterName: Option[String], databaseName: String, agg: AggregatedSource, repeats: Int, delay: FiniteDuration, end: DateTime)
                      (implicit fetcher: DataFetcher, ec: ExecutionContext): Future[Option[List[Double]]] =
    fetcher.fetchData(clusterName, databaseName, Aggregator.toInflux(agg.aggregator),
      agg.duration, agg.metricSource.metric, agg.metricSource.tags, repeats, delay, end)(ec)
}
