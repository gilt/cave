package com.cave.metrics.data

import java.security.MessageDigest

import com.cave.metrics.data.evaluator.Aggregator
import com.cave.metrics.data.evaluator.Aggregator.Aggregator
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object ParamHelper {
  def getString(name: String)(implicit params: Map[String, Seq[String]]): Option[String] = params.get(name) flatMap(_.headOption)

  def parseTags(tagString: Option[String]): Map[String, String] = tagString map { string =>
    (string.split(',') map { word =>
      word.split(':') match {
        case Array(key, value) => key -> value
      }
    }).toMap
  } getOrElse Map.empty[String, String]

  def parseDateTime(dateString: Option[String]): Option[DateTime] =
    dateString map ISODateTimeFormat.dateTime().parseDateTime

  def parseInt(numString: String): Option[Int] = {
    try {
      Some(numString.toInt)
    } catch {
      case _: NumberFormatException =>
        None
    }
  }

  def missing(param: String) = sys.error(s"Required parameter '$param' is missing.")

  def invalid(param: String, value: String) = sys.error(s"Parameter '$param' has invalid value '$value'.")

  def getMandatoryValue[T](param: String)(conversion: String => Option[T])
                          (implicit params: Map[String, Seq[String]]) =
    getString(param) map (name => conversion(name) getOrElse invalid(param, name)) getOrElse missing(param)
}

/**
 * A class to wrap a request for metric data
 *
 * @param metric      the name of the metric requested
 * @param tags        the tags of the metric
 * @param start       the start of the time range
 * @param end         the end of the time range
 * @param limit       an optional limit of number of data points
 */
case class MetricRequest(metric: String,
                         tags: Map[String, String],
                         aggregator: Aggregator,
                         period: FiniteDuration,
                         start: Option[DateTime], end: Option[DateTime],
                         limit: Option[Int])

object MetricRequest {

  def fromQueryString(implicit params: Map[String, Seq[String]]): Try[MetricRequest] = {
    import ParamHelper._

    Try(MetricRequest(
      metric = getString("metric") getOrElse missing("metric"),
      tags = parseTags(getString("tags")),
      aggregator = getMandatoryValue("aggregator")(Aggregator.withNameOpt),
      period = getMandatoryValue("period")(parseInt).minutes,
      start = parseDateTime(getString("start")),
      end = parseDateTime(getString("end")),
      limit = getString("limit") flatMap parseInt
    ))
  }
}

/**
 * A class to wrap a request for metric data evaluation
 *
 * @param condition   the metric condition to evaluate
 * @param start       the start of the time range
 * @param end         the end of the time range (optional, defaults to now)
 */
case class MetricCheckRequest(condition: String, start: DateTime, end: DateTime, interval: Int)

object MetricCheckRequest {

  def fromQueryString(implicit params: Map[String, Seq[String]]): Try[MetricCheckRequest] = {
    import ParamHelper._

    Try(MetricCheckRequest(
      condition = getString("condition") getOrElse missing("condition"),
      start = parseDateTime(getString("start")) getOrElse missing("start"),
      end = parseDateTime(getString("end")) getOrElse new DateTime(),
      interval = getString("interval") flatMap parseInt getOrElse 1
    ))
  }
}

/**
 * A class to wrap a sequence of metrics
 *
 * @param metrics  the sequence of metrics
 */
case class MetricBulk(metrics: Seq[Metric])

object MetricBulk {
  final val KeyMetrics = "metrics"

  implicit val metricsReads: Reads[MetricBulk] = (__ \ KeyMetrics).read[Seq[Metric]].map(MetricBulk.apply)
  implicit val metricsWrites = (__ \ KeyMetrics).write[Seq[Metric]].contramap { p: MetricBulk => p.metrics}
}

/**
 * A class to wrap one metric data point
 *
 * @param name       name of metric data point
 * @param timestamp  date/time of metric data point
 * @param value      value of metric data point
 * @param tags       tags of metric data point
 */

case class Metric(name: String, timestamp: Long, value: Double, tags: Map[String, String]) {

  /**
   * Compute a partition key for storing in Kinesis (128 characters)
   *
   * @return  a string representation of a SHA-512 hash of the string built from name + tags
   */
  def partitionKey: String = {
    val key = tags.foldLeft(name)(_ + _)
    val md = MessageDigest.getInstance("SHA-512")
    md.update(key.getBytes())
    md.digest().map("%02x" format _).mkString
  }

  final val MaximumStringLength = 128
  final val MaximumTagCount = 10

  def isSane: Boolean = {

    def isNotLong(string: String): Boolean = string.size < MaximumStringLength

    isNotLong(name) &&
      tags.size <= MaximumTagCount &&
      tags.foldLeft(true) { case (result, (name, value)) =>
        result && isNotLong(name) && isNotLong(value)
      }
  }
}

object Metric {
  final val KeyName = "name"
  final val KeyTags = "tags"
  final val KeyTimestamp = "timestamp"
  final val KeyValue = "value"

  final val Organization = "CAVE-account"
  final val Cluster = "CAVE-cluster"
  final val Error = "error"
  final val InternalError = "Internal Error"
  final val Internal = "cave-internal"

  implicit val metricReads: Reads[Metric] = (
    (__ \ KeyName).read[String] and
      (__ \ KeyTimestamp).read[Long] and
      (__ \ KeyValue).read[Double] and
      (__ \ KeyTags).read[Map[String, String]]
    )(Metric.apply _)


  implicit val metricWrites: Writes[Metric] = (
    (__ \ KeyName).write[String] and
      (__ \ KeyTimestamp).write[Long] and
      (__ \ KeyValue).write[Double] and
      (__ \ KeyTags).write[Map[String, String]]
    )(unlift(Metric.unapply))


  /**
   * Extract organization and team names from the host header
   *
   * @param hostname the host header for this request
   * @return the organization name and optional team name
   */
  def extractOrg(hostname: Option[String]): Try[(String, Option[String])] = hostname match {
    case Some(string) =>
      string.split('.') match {
        case Array(teamName, orgName, _, _) => Success((orgName, Some(teamName)))
        case Array(orgName, _, _) => Success((orgName, None))
        case _ => Failure(new RuntimeException(s"Invalid hostname format $string"))
      }

    case None => Failure(new RuntimeException("Hostname must be provided."))
  }

  def createDbName(orgName: String, teamName: Option[String]) = teamName match {
    case None => orgName
    case Some(name) => s"$name.$orgName"
  }
}