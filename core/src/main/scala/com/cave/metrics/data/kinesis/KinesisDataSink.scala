package com.cave.metrics.data.kinesis

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient
import com.amazonaws.services.kinesis.model.PutRecordRequest
import com.cave.metrics.data.{AwsConfig, Metric, SeqDataSink}
import org.apache.commons.logging.LogFactory
import play.api.libs.json.Json

import scala.util.Try
import scala.util.control.Exception._
import scala.util.control.NonFatal

class KinesisDataSink(config: AwsConfig, streamName: String) extends SeqDataSink {

  val log = LogFactory.getLog(classOf[KinesisDataSink])

  var client: Option[AmazonKinesisAsyncClient] = None

  /**
   * Connect to AWS Kinesis service
   */
  override def connect(): Unit = {
    client = Some({
      val c = new AmazonKinesisAsyncClient(config.awsCredentialsProvider)
      c.setEndpoint(config.awsKinesisConfig.endpoint, config.awsKinesisConfig.service, config.awsKinesisConfig.region)
      c
    })
    log.info("Kinesis Client connected.")
  }

  /**
   * Disconnect from AWS Kinesis service
   */
  override def disconnect(): Unit = {
    allCatch opt {
      client.foreach(_.shutdown())
    }
    client = None
    log.info("Kinesis Client disconnected.")
  }

  /**
   * Create a PutRecordRequest for this metric and send it to Kinesis
   *
   * @param metric the metric to send
   */
  override def sendMetric(metric: Metric): Unit = {

    def createRequest: PutRecordRequest = {
      val data = Json.toJson(metric).toString()
      log.info(s"Sending $data ...")

      val request = new PutRecordRequest
      request.setStreamName(streamName)
      request.setData(ByteBuffer.wrap(data.getBytes))
      request.setPartitionKey(metric.partitionKey)
      request
    }

    client foreach { c =>
      Try(c.putRecord(createRequest)) recover {
        case NonFatal(e) =>
          log.warn(s"Caught exception while talking to Kinesis: $e")
          throw e
      }
    }
  }
}
