package com.cave.metrics.data.evaluator

import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

abstract class AbstractEvaluator(conditionStr: String) extends AlertParser {

  private val condition = parseAll(anyAlert, conditionStr) match {
    case Success(SimpleAlert(leftOperand, operator, rightOperand, repeatCount, delay), _) =>
      Left((leftOperand, operator, rightOperand, repeatCount, delay))

    case Success(MissingDataAlert(metricSource, duration), _) =>
      Right((metricSource, duration))

    case _ => sys.error("Unsupported check condition: " + conditionStr)
  }

  def evaluateRange(clusterName: Option[String], databaseName: String, end: DateTime)
                   (implicit fetcher: DataFetcher, ec: ExecutionContext): Future[Try[Boolean]] = {
    condition match {
      case Left((left, operator, right, repeats, delay)) =>
        val results = for {
          leftResult <- evaluateSource(clusterName, databaseName, end, left, repeats, delay)(fetcher, ec)
          rightResult <- evaluateSource(clusterName, databaseName, end, right, repeats, delay)(fetcher, ec)
        } yield (leftResult, rightResult)

        results map {
          case (Some(l), Some(r)) =>
            val zipped = l.zip(r)
            implicit val op = operator
            scala.util.Success((zipped.size == repeats) && (zipped forall evaluatePair))

          case _ =>
            scala.util.Failure(new RuntimeException("Failed to evaluate: at least one series does not exist."))
        }

      case Right((metricSrc, duration)) =>
        getData(clusterName, databaseName, metricSrc.metric, metricSrc.tags, duration, end)(fetcher, ec) map {
          case Some(values) =>
            scala.util.Success(values.size == 0)

          case None => util.Failure(new RuntimeException("Cannot evaluate: series does not exist!"))
        }
    }
  }

  def evaluateSource(clusterName: Option[String], databaseName: String, end: DateTime,
                     source: Source, repeats: Int, delay: FiniteDuration)
                    (implicit fetcher: DataFetcher, ec: ExecutionContext): Future[Option[List[Double]]] =
    source match {
      case ValueSource(num) => Future.successful(Some(List.fill(repeats)(num)))
      case MetricSource(name, tags) => getData(clusterName, databaseName, name, tags, repeats, delay, end)(fetcher, ec)
      case a: AggregatedSource => getData(clusterName, databaseName, a, repeats, delay, end)(fetcher, ec)

      case FactoredSource(src, factor) => src match {
        case ValueSource(num) =>
          Future.successful(Some(List.fill(repeats)(num * factor)))

        case MetricSource(name, tags) =>
          getData(clusterName, databaseName, name, tags, repeats, delay, end)(fetcher, ec) map(_.map(_.map(_ * factor)))

        case a: AggregatedSource =>
          getData(clusterName, databaseName, a, repeats, delay, end)(fetcher, ec) map(_.map(_.map(_ * factor)))

        case _ => Future.failed(new RuntimeException("Impossible to evaluate."))
      }
    }

  def getData(clusterName: Option[String], databaseName: String, metricName: String,
                       metricTags: Map[String, String], repeats: Int, delay: FiniteDuration, end: DateTime)
                      (implicit fetcher: DataFetcher, ec: ExecutionContext): Future[Option[List[Double]]]

  def getData(clusterName: Option[String], databaseName: String, metricName: String,
                       metricTags: Map[String, String], duration: FiniteDuration, end: DateTime)
                      (implicit fetcher: DataFetcher, ec: ExecutionContext): Future[Option[List[Double]]]

  def getData(clusterName: Option[String], databaseName: String, agg: AggregatedSource, repeats: Int, delay: FiniteDuration, end: DateTime)
                      (implicit fetcher: DataFetcher, ec: ExecutionContext): Future[Option[List[Double]]]

  def evaluatePair(values: (Double, Double))(implicit op: Operator.Value): Boolean = op match {
    case Operator.LessThan            => values._1 <  values._2
    case Operator.LessThanOrEqual     => values._1 <= values._2
    case Operator.GreaterThan         => values._1 >  values._2
    case Operator.GreaterThanOrEqual  => values._1 >= values._2
    case Operator.Equal               => values._1 == values._2
    case Operator.NotEqual            => values._1 != values._2
  }
}
