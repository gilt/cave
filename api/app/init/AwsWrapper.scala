package init

import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import com.cave.metrics.data.Operation.Operation
import com.cave.metrics.data._
import com.cave.metrics.data.kinesis.KinesisDataSink
import com.cave.metrics.data.postgresql.PostgresDataManagerImpl
import org.apache.commons.logging.LogFactory
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import scala.concurrent._

class AwsWrapper(awsConfig: AwsConfig) {

  private final val log = LogFactory.getLog(this.getClass)

  val dataSink = new KinesisDataSink(awsConfig, awsConfig.rawStreamName)

  // a connection to the Postgres backend
  val dataManager: DataManager = new PostgresDataManagerImpl(awsConfig)

  // the SNS client
  val snsClient = {
    val c = new AmazonSNSAsyncClient(awsConfig.awsCredentialsProvider)
    c.setEndpoint(awsConfig.awsSNSConfig.endpoint)
    c
  }

  val topicArn = getTopicArn

  def getTopicArn = {
    val topicName = awsConfig.configurationChangesTopicName
    val arn = snsClient.createTopic(topicName).getTopicArn
    log.info(s"Topic $topicName has ARN $arn")
    arn
  }

  def init() = {
    dataSink.connect()
  }

  def shutdown() = {
    dataSink.disconnect()
  }

  def createOrganizationNotification(org: Organization) = sendOrganization(Operation.Create, org.name, "")
  def updateOrganizationNotification(org: Organization) = sendOrganization(Operation.Update, org.name, org.notificationUrl)
  def deleteOrganizationNotification(orgName: String) = sendOrganization(Operation.Delete, orgName, "")

  def createAlertNotification(schedule: Schedule) = sendAlert(Operation.Create, schedule)
  def updateAlertNotification(schedule: Schedule) = sendAlert(Operation.Update, schedule)
  def deleteAlertNotification(scheduleId: String, orgName: String) =
    sendNotification(Update(Entity.Alert, Operation.Delete, scheduleId, orgName))


  private[init] def sendOrganization(op: Operation, orgName: String, extra: String) =
    sendNotification(Update(Entity.Organization, op, orgName, extra))

  private[init] def sendAlert(op: Operation, schedule: Schedule) = {
    sendNotification(Update(Entity.Alert, op, schedule.alert.id.get, Json.stringify(Json.toJson(schedule))))
  }

  private[init] def sendNotification(update: Update): Future[Unit] = {
    val message = Json.stringify(Json.toJson(update))
    val request = new PublishRequest(topicArn, message)
    val response = snsClient publishAsync request
    future {
      blocking {
        val result: PublishResult = response.get()
        log.info(s"Successfully posted the notification '$message', messageId: ${result.getMessageId}")
      }
    }
  }
}
