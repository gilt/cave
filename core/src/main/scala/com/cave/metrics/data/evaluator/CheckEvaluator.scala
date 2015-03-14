package com.cave.metrics.data.evaluator

import com.cave.metrics.data.Check
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CheckEvaluator(check: Check) extends AbstractEvaluator(check.schedule.alert.condition) {

  def evaluate(fetcher: DataFetcher)(implicit ec: ExecutionContext): Future[Try[Boolean]] = {
    evaluateRange(clusterName = check.schedule.clusterName,
                  databaseName = check.schedule.databaseName,
                  end = check.timestamp)(fetcher, ec)
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
    fetcher.fetchData(clusterName, databaseName, agg.toString, Map.empty[String, String], repeats, delay, end)(ec)
}
