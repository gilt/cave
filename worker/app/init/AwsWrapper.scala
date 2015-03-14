package init

import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest}
import com.cave.metrics.data.kinesis.KinesisDataSink
import com.cave.metrics.data.{Check, AwsConfig}
import org.apache.commons.logging.LogFactory
import play.api.libs.json.Json

import scala.concurrent._
import scala.collection.JavaConverters._

object AwsWrapper {
  case class WorkItem(itemId: String, receiptHandle: String, check: Check)
}

class AwsWrapper(awsConfig: AwsConfig) {
  private final val Log = LogFactory.getLog(this.getClass)
  private final val MaxNumberOfMessages = 10

  val dataSink = new KinesisDataSink(awsConfig, awsConfig.rawStreamName)

  // the SQS client
  val sqsClient = {
    val c = new AmazonSQSAsyncClient(awsConfig.awsCredentialsProvider)
    c.setEndpoint(awsConfig.awsSQSConfig.endpoint)
    c
  }
  val queueName = awsConfig.alarmScheduleQueueName
  val queueUrl = sqsClient.createQueue(queueName).getQueueUrl
  Log.info(s"Queue $queueName has URL $queueUrl")

  import AwsWrapper._

  def init(): Unit = {
    dataSink.connect()
  }

  def shutdown(): Unit = {
    dataSink.disconnect()
  }

  /**
   * Poll the SQS queue and fetch work items
   *
   * @return a list of work items
   */
  def receiveMessages()(implicit ec: ExecutionContext): Future[List[WorkItem]] = {
    val request = new ReceiveMessageRequest()
      .withQueueUrl(queueUrl)
      .withMaxNumberOfMessages(MaxNumberOfMessages)
      .withWaitTimeSeconds(awsConfig.longPollTimeInSeconds)


    future {
      blocking {
        val result = sqsClient.receiveMessageAsync(request).get
        val items = result.getMessages.asScala map { message =>
          Log.debug(s"Received a message[${message.getMessageId}], with contents: ${message.getBody}")
          new WorkItem(
            itemId = message.getMessageId,
            receiptHandle = message.getReceiptHandle,
            check = Json.parse(message.getBody).as[Check])
        }
        items.toList
      }
    }
  }

  /**
   * Delete a processed message from the queue
   *
   * @param receiptHandle  the receipt handle of the message to delete
   * @return               true if the message was deleted, false otherwise
   */
  def deleteMessage(receiptHandle: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val request = new DeleteMessageRequest()
      .withQueueUrl(queueUrl)
      .withReceiptHandle(receiptHandle)

    future {
      blocking {
        val response = sqsClient.deleteMessageAsync(request)
        response.get()
        response.isDone
      }
    }
  }
}
