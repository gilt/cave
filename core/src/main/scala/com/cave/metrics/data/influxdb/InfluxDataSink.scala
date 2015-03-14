package com.cave.metrics.data.influxdb

import com.cave.metrics.data.{DataSink, Metric}

class InfluxDataSink(config: InfluxConfiguration) extends DataSink {

  val influxClientFactory = new InfluxClientFactory(config)

  // using HTTP interface, which is connectionless
  override def connect(): Unit = {}
  override def disconnect(): Unit = {}

  override def sendMetrics(metrics: Seq[Metric]): Unit = influxClientFactory.sendMetrics(metrics)
}
