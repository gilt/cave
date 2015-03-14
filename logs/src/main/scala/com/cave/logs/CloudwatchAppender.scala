package com.cave.logs

import java.net.InetAddress
import java.util.{ArrayList => JArrayList, List => JList}

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.spi.LogbackLock
import ch.qos.logback.core.{Layout, UnsynchronizedAppenderBase}
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.logs.AWSLogsClient
import com.amazonaws.services.logs.model._

import scala.beans.BeanProperty
import scala.util.control.NonFatal

class CloudWatchAppender extends UnsynchronizedAppenderBase[LoggingEvent] {

  @BeanProperty var awsAccessKey: String = _
  @BeanProperty var awsSecretKey: String = _
  @BeanProperty var logGroupName: String = _
  @BeanProperty var layout: Layout[LoggingEvent] = _
  @BeanProperty var retentionDays: Int = 7

  private[this] val lock = new LogbackLock
  private[this] var sequenceToken: String = null

  lazy val logStreamName = InetAddress.getLocalHost.getCanonicalHostName

  val logsClient = new AWSLogsClient(new AWSCredentials() {
    override def getAWSAccessKeyId: String = awsAccessKey
    override def getAWSSecretKey: String = awsSecretKey
  })

  override def append(event: LoggingEvent): Unit = {
    if (event.getLevel == Level.WARN || event.getLevel == Level.ERROR) {
      lock.synchronized {
        try {
          val message = if (layout == null) event.getFormattedMessage else layout.doLayout(event)

          val request = new PutLogEventsRequest()
            .withLogGroupName(logGroupName)
            .withLogStreamName(logStreamName)
            .withLogEvents(
              new InputLogEvent()
                .withTimestamp(event.getTimeStamp)
                .withMessage(message))
            .withSequenceToken(sequenceToken)

          sequenceToken = logsClient.putLogEvents(request).getNextSequenceToken
        } catch {
          case NonFatal(e) =>
            addError("Failed to send CWLogs event", e)
        }
      }
    }
  }

  override def start(): Unit = {
    println(s"Attempting to start CloudWatchAppender for '$logGroupName'...")

    if (layout == null) {
      addWarn(s"No layout provided. Will send only the log message.")
    }

    if (createLogGroup() && applyRetentionPolicy() && createLogStream()) {
      println(s"Started CloudWatchAppender for '$logGroupName'.")
      super.start()
    }
  }

  private[this] def createLogGroup(): Boolean = {
    try {
      logsClient.createLogGroup(new CreateLogGroupRequest(logGroupName))
      addInfo(s"Log Group $logGroupName created.")
      true
    } catch {
      case e: ResourceAlreadyExistsException =>
        addInfo(s"Log Group $logGroupName already exists.")
        true

      case e: AmazonServiceException =>
        addWarn(s"Unable to create CloudWatch Log Group '$logGroupName'", e)
        false

      case NonFatal(e) =>
        addWarn(s"Unknown error while creating CloudWatch Log Group '$logGroupName'", e)
        false
    }
  }

  private def applyRetentionPolicy(): Boolean = {
    try {
      logsClient.putRetentionPolicy(new PutRetentionPolicyRequest(logGroupName, retentionDays))
      true
    } catch {
      case e: AmazonServiceException =>
        addWarn(s"Unable to apply retention policy to CloudWatch Log Group '$logGroupName'", e)
        false

      case NonFatal(e) =>
        addWarn(s"Unknown error while applying retention policy to CloudWatch Log Group '$logGroupName'", e)
        false
    }
  }

  private[this] def createLogStream(): Boolean = {
    try {
      logsClient.createLogStream(new CreateLogStreamRequest(logGroupName, logStreamName))
      addInfo(s"Log Stream '$logStreamName' created for group '$logGroupName'.")
      true
    } catch {
      case e: ResourceAlreadyExistsException =>
        addInfo(s"Log Stream '$logStreamName' created for group '$logGroupName'.")
        true

      case e: AmazonServiceException =>
        addWarn(s"Unable to create stream '$logStreamName' for group '$logGroupName'.", e)
        false

      case NonFatal(e) =>
        addWarn(s"Unknown error while creating CloudWatch Log Stream '$logStreamName' for group '$logGroupName'.", e)
        false
    }
  }
}
