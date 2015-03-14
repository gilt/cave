package com.cave.metrics.data

import play.api.libs.json.Json

/**
 * The Data Sink trait
 */
trait DataSink {
  def connect(): Unit
  def sendMetrics(metrics: Seq[Metric]): Unit
  def disconnect(): Unit
}

/**
 * Sequence Data Producer
 *
 * Sending a list of metrics one by one
 */
abstract class SeqDataSink extends DataSink {

  override def sendMetrics(metrics: Seq[Metric]): Unit = metrics foreach sendMetric

  private[data] def sendMetric(metric: Metric): Unit
}

/**
 * Data Sink that requires no connection
 */
sealed abstract class ConnectionlessSeqDataSink extends SeqDataSink {
  override def disconnect(): Unit = Unit
  override def connect(): Unit = Unit
}

/**
 * /dev/null producer -> ignore all data
 *
 * Useful for tests
 */
object NullDataSink extends ConnectionlessSeqDataSink {
  override def sendMetric(metric: Metric): Unit = Unit
}

/**
 * Console producer -> print all data to console
 *
 * Useful for testing
 */
object ConsoleDataSink extends ConnectionlessSeqDataSink {
  override def sendMetric(metric: Metric): Unit = println(s"Metric: ${Json.toJson(metric).toString()}")
}
