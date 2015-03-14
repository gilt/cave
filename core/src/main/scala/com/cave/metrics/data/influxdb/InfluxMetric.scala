package com.cave.metrics.data.influxdb

import com.cave.metrics.data.Metric

object InfluxMetric {

  final val MissingAccountTagMessage = "All metrics should have the account tag"

  /**
   * Convert a sequence of metrics into InfluxDB requests
   *
   * @param metrics the metrics to send to InfluxDB
   * @return requests, grouped by account
   */
  def prepareRequests(metrics: Seq[Metric]): Map[String, String] = {
    metrics.groupBy(getAccount).map { case (account, metricSeq) =>
      account ->
        metricSeq.groupBy(getTagNames)
          .values.map(convertToJson)
          .mkString("[", ",", "]")
    }
  }

  private def getTagNames(metric: Metric): String =
    metric.tags.keys.foldLeft(metric.name)(_ + _)

  private def getAccount(metric: Metric): String =
    metric.tags.getOrElse(Metric.Organization, sys.error(MissingAccountTagMessage))

  /**
   * Convert a sequence of metrics to InfluxDB JSON format
   *
   * Assumes all metrics have the same partition key
   *
   * @param metrics the metrics to convert
   * @return the JSON string for these metrics
   */
  private[data] def convertToJson(metrics: Seq[Metric]): String = {

    def wrapSeq(words: Seq[String]): String = words.map(word => s""","$word"""").mkString("")

    def nonCaveTags(metric: Metric): Map[String, String] = metric.tags.filterKeys { key =>
      key != Metric.Organization && key != Metric.Cluster
    }

    val first = metrics.headOption.getOrElse(sys.error("There should be at least one metric to convert."))

    val points = metrics.map { metric =>
      s"[${metric.timestamp},${metric.value}${wrapSeq(nonCaveTags(metric).values.toSeq)}]"
    }.mkString("[", ",", "]")

    val columns = s""""time","value"${wrapSeq(nonCaveTags(first).keys.toSeq)}"""

    s"""{"name":"${first.name}","columns":[$columns],"points":$points}"""
  }
}