package init

import java.net.InetAddress

import com.amazonaws.AmazonClientException
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{DescribeInstancesRequest, Instance}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sns.util.Topics
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest}
import com.cave.metrics.data.{AwsConfig, Check, Update}
import com.fasterxml.jackson.core.JsonParseException
import org.apache.commons.logging.LogFactory
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.concurrent._

object AwsWrapper {
  case class WorkItem(itemId: String, receiptHandle: String, update: Update)
}


class AwsWrapper(awsConfig: AwsConfig) {

  private final val Log = LogFactory.getLog(this.getClass)

  private[init] val autoScalingClient = {
    val c = new AmazonAutoScalingClient(awsConfig.awsCredentialsProvider)
    c.setEndpoint(awsConfig.awsAutoScalingConfig.endpoint)
    c
  }

  private[init] val ec2Client = {
    val c = new AmazonEC2Client(awsConfig.awsCredentialsProvider)
    c.setEndpoint(awsConfig.awsEC2Config.endpoint)
    c
  }

  // the SNS client
  val snsClient = {
    val c = new AmazonSNSAsyncClient(awsConfig.awsCredentialsProvider)
    c.setEndpoint(awsConfig.awsSNSConfig.endpoint)
    c
  }
  val topicName = awsConfig.configurationChangesTopicName
  val topicArn = snsClient.createTopic(topicName).getTopicArn
  Log.warn(s"Topic $topicName has ARN $topicArn")

  // the SQS client
  val sqsClient = {
    val c = new AmazonSQSAsyncClient(awsConfig.awsCredentialsProvider)
    c.setEndpoint(awsConfig.awsSQSConfig.endpoint)
    c
  }
  val queueName = s"scheduler-${InetAddress.getLocalHost.getHostName}".replace(".", "-")
  Log.warn(s"About to create a queue named $queueName...")
  val queueUrl = sqsClient.createQueue(queueName).getQueueUrl
  Log.warn(s"Queue $queueName has URL $queueUrl")

  val subscriptionArn = Topics.subscribeQueue(snsClient, sqsClient, topicArn, queueUrl)
  Log.warn(s"Subscribed our queue to the topic, subscriptionArn: $subscriptionArn")

  val workerQueueName = awsConfig.alarmScheduleQueueName
  val workerQueueUrl = sqsClient.createQueue(workerQueueName).getQueueUrl
  Log.warn(s"Worker queue $workerQueueName has URL $workerQueueUrl")

  def deleteQueue() {
    Log.warn("Removing subscription from topic...")
    snsClient.unsubscribe(subscriptionArn)
    Log.warn("Done.")

    Log.info(s"Deleting my queue $queueName...")
    sqsClient.deleteQueue(queueUrl)
    Log.warn(s"Done.")
  }

  import init.AwsWrapper._

  /**
   * Poll the SQS queue and fetch work items
   *
   * @return a list of work items
   */
  def receiveMessages()(implicit ec: ExecutionContext): Future[List[WorkItem]] = {
    val request = new ReceiveMessageRequest()
      .withQueueUrl(queueUrl)
      .withWaitTimeSeconds(awsConfig.longPollTimeInSeconds)


    future {
      blocking {
        try {
          val result = sqsClient.receiveMessageAsync(request).get
          val items = result.getMessages.asScala map { message =>
            val msg = (Json.parse(message.getBody) \ "Message").as[String]
            Log.warn(s"Received a message[${message.getMessageId}], with contents: $msg")
            try {
              val update = Json.parse(msg).as[Update]
              Some(new WorkItem(message.getMessageId, message.getReceiptHandle, update))
            } catch {
              case e: JsonParseException =>
                Log.error(s"Failed to parse an update from $message.\nException: $e")
                deleteMessage(message.getReceiptHandle)
                None
            }
          }
          items.filter(_.isDefined).map(_.get).toList
        } catch {
          case e: AmazonClientException =>
            Log.warn(s"Failed to receive cache update messages, caused by $e")
            List.empty[WorkItem]
        }
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

    Log.warn(s"Deleting message with handle $receiptHandle")

    future {
      blocking {
        try {
          val response = sqsClient.deleteMessageAsync(request)
          response.get()
          response.isDone
        } catch {
          case e: AmazonClientException =>
            Log.warn(s"Failed to delete cache update message, caused by $e")
            false
        }
      }
    }
  }

  /**
   * Sends a scheduled alert check to the workers
   *
   * @param check  the scheduled alert check
   * @param ec     the execution context
   * @return       true if successfully sent, false otherwise
   */
  def sendMessage(check: Check)(implicit ec: ExecutionContext): Future[Boolean] = {
    future {
      blocking {
        try {
          val message = Json.stringify(Json.toJson(check))
          Log.warn(s"Sending a message to worker: $message")
          sqsClient.sendMessage(workerQueueUrl, message) != null
        } catch {
          case e: AmazonClientException =>
            Log.warn(s"Failed to send scheduled alert check, caused by $e")
            false
        }
      }
    }
  }

  /**
   * Return list of DNS names for instances in the auto scaling group
   *
   * @param serviceName   the value of the service tag to find
   * @return              list of DNS names for the instances in the group
   */
  def getNodes(serviceName: String): List[String] =
    getAutoScalingGroupsForService(serviceName) flatMap { group =>
      (group.getInstances.asScala flatMap { instance =>
        getInstanceById(instance.getInstanceId) map (_.getPrivateDnsName)
      }).toList
    }

  /**
   * Find the auto scaling group whose 'service' tag has the given value
   *
   * @param serviceName  the service we are looking for
   * @return             the AutoScalingGroup object, if found
   */
  private[init] def getAutoScalingGroupsForService(serviceName: String): List[AutoScalingGroup] =
    Option(autoScalingClient.describeAutoScalingGroups()) map { result =>
      (result.getAutoScalingGroups.asScala filter { group =>
        group.getTags.asScala exists (tag => tag.getKey == "service" && tag.getValue == serviceName)
      }).toList
    } getOrElse List.empty[AutoScalingGroup]

  /**
   * Find the instance with the given identifier
   *
   * @param instanceId  the instance we are looking for
   * @return            the instance object, if found; None, otherwise
   */
  private[init] def getInstanceById(instanceId: String): Option[Instance] =
    Option(ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId))) flatMap { result =>
      result.getReservations.asScala.headOption flatMap (_.getInstances.asScala.headOption)
    }
}
