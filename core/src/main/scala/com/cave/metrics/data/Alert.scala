package com.cave.metrics.data

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * The configuration for an alert
 *
 * @param id             the alert identifier
 * @param description    the alert description
 * @param enabled        if the alert should be enabled or not
 * @param period         how often to check the condition
 * @param condition      the condition to evaluate
 * @param handbookUrl    link to a handbook to help resolve this alert
 * @param routing        optional alert routing information
 * @param relatedMetrics data for metrics related to this alert
 */
case class Alert(id: Option[String], description: String, enabled: Boolean, period: String, condition: String, handbookUrl: Option[String], routing: Option[Map[String, String]],
                 relatedMetrics: Option[Set[AlertMetric]] = None) {
  lazy val routingStr = routing map { v => Json.stringify(Json.toJson(v)) }
}

object Alert {

  final val KeyId = "id"
  final val KeyDescription = "description"
  final val KeyEnabled = "enabled"
  final val KeyPeriod = "period"
  final val KeyCondition = "condition"
  final val KeyHandbookUrl = "handbook_url"
  final val KeyRouting = "routing"
  final val KeyRelatedMetrics = "related_metrics"

  implicit val alertReads: Reads[Alert] = (
    (__ \ KeyId).readNullable[String] and
      (__ \ KeyDescription).read[String] and
      (__ \ KeyEnabled).read[Boolean] and
      (__ \ KeyPeriod).read[String] and
      (__ \ KeyCondition).read[String] and
      (__ \ KeyHandbookUrl).readNullable[String] and
      (__ \ KeyRouting).readNullable[Map[String, String]] and
      (__ \ KeyRelatedMetrics).readNullable[Set[AlertMetric]]
    )(Alert.apply _)

  implicit val alertWrites: Writes[Alert] = (
    (__ \ KeyId).writeNullable[String] and
      (__ \ KeyDescription).write[String] and
      (__ \ KeyEnabled).write[Boolean] and
      (__ \ KeyPeriod).write[String] and
      (__ \ KeyCondition).write[String] and
      (__ \ KeyHandbookUrl).writeNullable[String] and
      (__ \ KeyRouting).writeNullable[Map[String, String]] and
      (__ \ KeyRelatedMetrics).writeNullable[Set[AlertMetric]]
    )(unlift(Alert.unapply))

  def routingFromString(json: Option[String]): Option[Map[String, String]] =
    json map { v => Json.parse(v).as[Map[String, String]] }

  def routingAsStr(routing: Option[Map[String, String]]): Option[String] =
    routing map { v => Json.stringify(Json.toJson(v)) }
}

/**
 * A metric related to an alert
 *
 * @param name            the name of the metric
 * @param tags            tags and their values associated with this metric
 * @param aggregator      the type of aggregation done (only applicable for Aggregated metrics)
 * @param periodSeconds   the time in seconds that the metric is aggregated over (only applicable for Aggregated metrics)
 */
case class AlertMetric(name: String, tags: Map[String, String], aggregator: Option[String], periodSeconds: Option[Long])

object AlertMetric {
  final val KeyName = "name"
  final val KeyTags = "tags"
  final val KeyAggregator = "aggregator"
  final val KeyPeriod = "period_seconds"

  implicit val alertMetricReads: Reads[AlertMetric] = (
    (__ \ KeyName).read[String] and
      (__ \ KeyTags).read[Map[String, String]] and
      (__ \ KeyAggregator).readNullable[String] and
      (__ \ KeyPeriod).readNullable[Long]
    )(AlertMetric.apply _)

  implicit val alertMetricWrites: Writes[AlertMetric] = (
    (__ \ KeyName).write[String] and
      (__ \ KeyTags).write[Map[String, String]] and
      (__ \ KeyAggregator).writeNullable[String] and
      (__ \ KeyPeriod).writeNullable[Long]
  )(unlift(AlertMetric.unapply))
}

/**
 * Class to encapsulate changes that can be done to an alert
 *
 * @param description  the alert description
 * @param enabled      if the alert should be enabled or not
 * @param period       how often to check the alert condition
 */
case class AlertPatch(description: Option[String], enabled: Option[Boolean], period: Option[String], handbookUrl: Option[String], routing: Option[Map[String, String]])

object AlertPatch {
  final val KeyDescription = "description"
  final val KeyEnabled = "enabled"
  final val KeyPeriod = "period"
  final val KeyHandbookUrl = "handbook_url"
  final val KeyRouting = "routing"

  implicit val alertPatchReads: Reads[AlertPatch] = (
      (__ \ KeyDescription).readNullable[String] and
      (__ \ KeyEnabled).readNullable[Boolean] and
      (__ \ KeyPeriod).readNullable[String] and
      (__ \ KeyHandbookUrl).readNullable[String] and
      (__ \ KeyRouting).readNullable[Map[String, String]]
    )(AlertPatch.apply _)
}
