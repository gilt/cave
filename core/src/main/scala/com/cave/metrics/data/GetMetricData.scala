package com.cave.metrics.data

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat._
import play.api.libs.json._

case class MetricData(time: DateTime, value: Double)
case class MetricDataBulk(metrics: Seq[MetricData])
case class MetricInfo(name: String, tags: List[String])

object MetricData {

  implicit val datetimeReads: Reads[DateTime] =  __.read[String].map(dateTimeParser.parseDateTime)
  implicit val datetimeWrites = new Writes[DateTime] {
    def writes(value: DateTime) = JsString(dateTimeNoMillis.print(value))
  }

  implicit val metricDataReads = Json.reads[MetricData]
  implicit val metricDataWrites = Json.writes[MetricData]
}

object MetricDataBulk {
  implicit val metricDataBulkReads = Json.reads[MetricDataBulk]
  implicit val metricDataBulkWrites = Json.writes[MetricDataBulk]
}

object MetricInfo {
  implicit val metricInfoReads = Json.reads[MetricInfo]
  implicit val metricInfoWrites = Json.writes[MetricInfo]
}

trait GetMetricData {

  /**
   * Fetch metric data
   *
   * @param metric    the name of metric to fetch
   * @param tags      the tags for the metric
   * @param timeRange the start/end of a time range
   * @return          the data, keyed on time
   */
  def getMetricData(metric: String, tags: Map[String, String], timeRange: (DateTime, DateTime)): MetricDataBulk
}
