package init

import java.util.concurrent.TimeUnit

import com.cave.metrics.data.influxdb.{InfluxClientFactory, InfluxConfiguration}
import com.cave.metrics.data.metrics.InternalReporter
import com.cave.metrics.data.{AlertManager, AwsConfig, Metric, PasswordHelper}
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet}
import com.typesafe.config.ConfigFactory
import org.apache.commons.logging.LogFactory
import play.api.Play

object Init {


  val metricRegistry = new MetricRegistry

  private val log = LogFactory.getLog("Init")
  private val InternalTags = Map(Metric.Organization -> Metric.Internal)

  private[this] val configuration = Play.current.configuration
  val baseUrl = configuration.getString("baseUrl").getOrElse("https://api.cavellc.io")
  val maxTokens = configuration.getInt("maxTokens").getOrElse(3)
  val serviceConfFile = configuration.getString("serviceConf").getOrElse("api-service.conf")

  val appConfig = ConfigFactory.load(serviceConfFile).getConfig("api-service")

  // prepare AWS config and Kinesis data sink
  val awsConfig = new AwsConfig(appConfig)

  // a wrapper for required AWS
  val awsWrapper = new AwsWrapper(awsConfig)

  // a connection to the InfluxDB backend
  val influxConfig = appConfig.getConfig("influx")

  val influxClientFactory = new InfluxClientFactory(InfluxConfiguration(influxConfig))

  val alertManager = new AlertManager(awsWrapper.dataManager, influxClientFactory)

  val mailService = new MailService
  val passwordHelper = new PasswordHelper

  def init() {
    awsWrapper.init()
    log.warn("Init.init()")

    val reporter = InternalReporter(registry = metricRegistry) { metrics =>
      metrics foreach(metric => awsWrapper.dataSink.sendMetric(Metric(metric.name, metric.timestamp, metric.value, InternalTags ++ metric.tags)))
    }

    reporter.start(1, TimeUnit.MINUTES)

    metricRegistry.register(MetricRegistry.name("jvm", "gc"), new GarbageCollectorMetricSet())
    metricRegistry.register(MetricRegistry.name("jvm", "memory"), new MemoryUsageGaugeSet())
    metricRegistry.register(MetricRegistry.name("jvm", "thread-states"), new ThreadStatesGaugeSet())
  }

  def shutdown() {
    awsWrapper.shutdown()
    influxClientFactory.close()
    log.warn("Init.shutdown()")
  }
}
