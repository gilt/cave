package init

import akka.actor._
import com.cave.metrics.data.AwsConfig
import com.cave.metrics.data.influxdb.{InfluxClientFactory, InfluxConfiguration}
import com.typesafe.config.ConfigFactory
import org.apache.commons.logging.LogFactory
import play.api.Play
import worker.Coordinator
import worker.converter.ConverterFactory
import worker.web.AsyncNotificationSender

object Init {

  private[this] val configuration = Play.current.configuration
  private val log = LogFactory.getLog("Init")

  val serviceConfFile = configuration.getString("serviceConf").getOrElse("worker.conf")

  val appConfig = ConfigFactory.load(serviceConfFile).getConfig("worker")

  // prepare AWS config
  val awsConfig = new AwsConfig(appConfig)

  // a wrapper for required AWS
  val awsWrapper = new AwsWrapper(awsConfig)

  // a connection to the InfluxDB backend
  val influxConfig = appConfig.getConfig("influx")
  val influxClientFactory = new InfluxClientFactory(InfluxConfiguration(influxConfig))

  val converterFactory = new ConverterFactory(appConfig.getConfig("converters"))
  val sender = new AsyncNotificationSender(converterFactory)

  val system = ActorSystem("CaveWorker")
  val coordinator = system.actorOf(Props(new Coordinator(awsWrapper)), "coordinator")

  def init() {
    log.info("Init started...")
    awsWrapper.init()
    log.info("Init completed.")
  }

  def shutdown() {
    log.info("Shutdown started...")
    awsWrapper.shutdown()
    influxClientFactory.close()
    system.shutdown()
    log.info("Shutdown completed.")
  }
}
