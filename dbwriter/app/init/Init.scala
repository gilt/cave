package init

import java.net.InetAddress
import java.util.UUID

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.cave.metrics.data.AwsConfig
import com.cave.metrics.data.influxdb.{InfluxConfiguration, InfluxDataSink}
import com.cave.metrics.data.kinesis.RecordProcessorFactory
import com.typesafe.config.ConfigFactory
import org.apache.commons.logging.LogFactory
import play.api.Play

import scala.util.Try

object Init {

  // Docker should place the stream name in this environment variable
  final val EnvStreamName = "STREAM_NAME"

  // The name of this application for Kinesis Client Library
  final val ApplicationName = "cave-db-worker"

  // CloudWatch Reporter parameters
  final val MetricsNamespace = s"metrics-$ApplicationName"
  final val MetricsBufferTime = 1000L
  final val MetricsBufferSize = 200

  final val ThreadWaitTimeout = 10000L

  private val Log = LogFactory.getLog("db-writer-app")

  val worker = createWorker()
  val workerThread = new Thread(worker)

  def start(): Unit = {
    workerThread.start()
  }

  def shutdown(): Unit = {
    worker.shutdown()
    Try (workerThread.join(ThreadWaitTimeout)) recover {
      case e: Exception =>
        Log.info(s"Caught exception while joining worker thread: $e")
    }
  }

  /**
   * Create a suitable Worker object for the task at hand
   *
   * @return the Worker object
   */
  private[this] def createWorker(): Worker = {
    val configuration = Play.current.configuration
    val serviceConfFile = configuration.getString("serviceConf").getOrElse("db-writer-service.conf")
    val kinesisAppName = configuration.getString("appName").getOrElse(ApplicationName)
    val appConfig = ConfigFactory.load(serviceConfFile).getConfig("db-writer")
    val awsConfig = new AwsConfig(appConfig)

    val streamName = System.getenv(EnvStreamName) match {
      case "processed" => awsConfig.processedStreamName
      case _ => awsConfig.rawStreamName
    }

    val workerId = s"${InetAddress.getLocalHost.getCanonicalHostName}:${UUID.randomUUID()}"

    Log.info(s"Running $ApplicationName for stream $streamName as worker $workerId")

    // a connection to the InfluxDB backend
    val influxConfig = appConfig.getConfig("influx")

    new Worker(
      // a factory for record processors
      new RecordProcessorFactory(
        awsConfig,
        new InfluxDataSink(InfluxConfiguration(influxConfig))),

      // a client library instance
      new KinesisClientLibConfiguration(kinesisAppName, streamName, awsConfig.awsCredentialsProvider, workerId)
        .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON),

      new NullMetricsFactory)
      // TODO: check out the possibility to use CloudWatch Metrics
      // new CWMetricsFactory(awsConfig.awsCredentialsProvider, MetricsNamespace, MetricsBufferTime, MetricsBufferSize))
  }
}
