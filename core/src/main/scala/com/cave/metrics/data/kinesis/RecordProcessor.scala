package com.cave.metrics.data.kinesis

import java.util.{List => JList}

import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.model.Record
import com.cave.metrics.data._
import org.apache.commons.logging.LogFactory
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

class RecordProcessor(config: AwsConfig, sink: DataSink) extends IRecordProcessor with ExponentialBackOff {

  private[this] var shardId: String = _
  private var nextCheckpointTimeMillis: Long = _

  private[this] val log = LogFactory.getLog(classOf[RecordProcessor])

  // Back off and retry settings for checkpoint
  override val MaxBackOffTimeInMillis = 10000L
  override val ShouldLogErrors: Boolean = true
  private val NumRetries = 10
  private val CheckpointIntervalInMillis = 1000L

  override def initialize(shardId: String): Unit = {
    this.shardId = shardId
  }

  override def shutdown(check: IRecordProcessorCheckpointer, reason: ShutdownReason): Unit = {
    if (reason == ShutdownReason.TERMINATE) {
      checkpoint(check)
    }
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  override def processRecords(records: JList[Record], check: IRecordProcessorCheckpointer): Unit = {
    val metrics = (records.asScala map convert).filter(_.isSuccess)
    if (metrics.size == records.size()) {
      // all metrics successfully converted
      log.info(s"Received $metrics")
      sink.sendMetrics(for (Success(metric) <- metrics) yield metric)
    } else {
      log.error("Failed to parse records into Metric objects.")
    }

    if (System.currentTimeMillis() > nextCheckpointTimeMillis) {
      checkpoint(check)
      nextCheckpointTimeMillis = System.currentTimeMillis() + CheckpointIntervalInMillis
    }
  }

  private[this] def convert(record: Record): Try[Metric] =
    Try (Json.parse(new String(record.getData.array())).as[Metric])

  private[this] def checkpoint(check: IRecordProcessorCheckpointer): Unit = {
    Try {
      retryUpTo(NumRetries) {
        check.checkpoint()
      }
    } recover {
      case e: Exception =>
        log.warn(s"Failed to checkpoint shard ${shardId}: ${e.getMessage}")
    }
  }
}