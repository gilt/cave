package com.cave.metrics.data.influxdb

import java.util.concurrent.Executors

import com.cave.metrics.data.Metric
import com.typesafe.config.Config
import org.joda.time.{DateTimeZone, DateTime}
import collection.JavaConversions._
import scala.concurrent.ExecutionContext

case class InfluxConfiguration(default: InfluxClusterConfig, alternates: Map[String, InfluxClusterConfig]) {

  val alts = alternates.map { case (name, config) => s"Name: $name, Config: $config"}
  println(s"Default: $default, Alters: $alts")
}

object InfluxConfiguration {

  def apply(config: Config) = {

    val default = InfluxClusterConfig(config.getString("url"), config.getString("user"), config.getString("pass"))

    val alternates = config.getConfigList("alternates") map { conf =>
      conf.getString("name") -> InfluxClusterConfig(conf.getString("url"), default.user, default.pass)
    }

    new InfluxConfiguration(default, alternates.toMap)
  }
}

class InfluxClientFactory(config: InfluxConfiguration) {

  def createClient(clusterConfig: InfluxClusterConfig): (InfluxClient, ExecutionContext) =
    new InfluxClient(clusterConfig) -> ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  val defaultClient = createClient(config.default)
  val alternates = config.alternates map { case (name, clusterConfig) => name -> createClient(clusterConfig)}

  def getClient(name: Option[String]): (InfluxClient, ExecutionContext) = name match {
    case None => defaultClient
    case Some(clusterName) => alternates.getOrElse(clusterName, default = defaultClient)
  }

  def sendMetrics(metrics: Seq[Metric]): Unit = {

    val now = new DateTime().withZone(DateTimeZone.UTC).getMillis / 1000
    val maxDelay = metrics.foldLeft(0L) { case (delay, metric) =>
        Math.max(delay, Math.abs(metric.timestamp - now))
    }
    val (defaultClient, defaultContext) = getClient(None)
    defaultClient.putMetricData(Seq(
      Metric("writer-delay", now, maxDelay, Map(Metric.Organization -> Metric.Internal))
    ))(defaultContext)

    metrics.groupBy(_.tags.get(Metric.Cluster)) map { case (cluster, metricSeq) =>
      val (client, context) = getClient(cluster)
      client.putMetricData(metricSeq)(context)
    }
  }

  def close(): Unit = {
    defaultClient._1.close()
    alternates map { case (_, (client, _)) => client.close() }
  }
}
