package com.cave.metrics.data

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider, BasicAWSCredentials, ClasspathPropertiesFileCredentialsProvider}
import com.typesafe.config.Config

trait AmazonWebServiceConfig {
  def endpoint: String
  def service: String
  def region: String
}

class AwsConfig(config: Config) {

  private lazy val awsConfig = config.resolve.getConfig("aws")
  private lazy val rdsConfig = awsConfig.resolve.getConfig("rds")

  private lazy val awsCredentialsConfig = awsConfig.getConfig("credentials")
  lazy val awsCredentialsProvider = createAwsCredentialsProvider(
    awsCredentialsConfig.getString("access-key"),
    awsCredentialsConfig.getString("secret-key"))

  println("AWS Access Key: " + awsCredentialsProvider.getCredentials.getAWSAccessKeyId)

  private lazy val kinesisConfig = awsConfig.getConfig("kinesis")
  lazy val awsKinesisConfig = makeAmazonWebServiceConfig(kinesisConfig)

  private lazy val awsKinesisStreamConfig = kinesisConfig.getConfig("stream")
  lazy val rawStreamName = awsKinesisStreamConfig.getString("raw")
  lazy val processedStreamName = awsKinesisStreamConfig.getString("processed")

  private lazy val sqsConfig = awsConfig.getConfig("sqs")
  lazy val awsSQSConfig = makeAmazonWebServiceConfig(sqsConfig)

  lazy val longPollTimeInSeconds = sqsConfig.getInt("longPollTimeInSeconds")

  private lazy val awsSqsQueuesConfig = sqsConfig.getConfig("queues")
  lazy val configurationChangesQueueName = awsSqsQueuesConfig.getString("config-changes")
  lazy val alarmScheduleQueueName = awsSqsQueuesConfig.getString("alarm-schedule")

  private lazy val autoScalingConfig = awsConfig.getConfig("autoscaling")
  lazy val awsAutoScalingConfig = makeAmazonWebServiceConfig(autoScalingConfig)

  private lazy val ec2Config = awsConfig.getConfig("ec2")
  lazy val awsEC2Config = makeAmazonWebServiceConfig(ec2Config)

  private lazy val snsConfig = awsConfig.getConfig("sns")
  lazy val awsSNSConfig = makeAmazonWebServiceConfig(snsConfig)

  private lazy val awsSnsTopicsConfig = snsConfig.getConfig("topics")
  lazy val configurationChangesTopicName = awsSnsTopicsConfig.getString("config-changes")

  lazy val rdsJdbcDatabaseClass = rdsConfig.getString("database-class")
  lazy val rdsJdbcDatabaseUrl = rdsConfig.getString("database-jdbc")
  lazy val rdsJdbcDatabaseServer = rdsConfig.getString("database-server")
  lazy val rdsJdbcDatabasePort = rdsConfig.getString("database-port")
  lazy val rdsJdbcDatabaseName = rdsConfig.getString("database-name")
  lazy val rdsJdbcDatabaseUser = rdsConfig.getString("database-user")
  lazy val rdsJdbcDatabasePassword = rdsConfig.getString("database-password")
  lazy val rdsJdbcDatabasePoolSize = rdsConfig.getInt("pool-size")
  lazy val rdsJdbcConnectionTimeout = rdsConfig.getInt("connection-timeout")

  lazy val leadershipTermTimeoutSeconds = config.getInt("leadershipTermTimeoutSeconds")
  lazy val leadershipTermLengthSeconds = config.getInt("leadershipTermLengthSeconds")

  /**
   * Create a credentials provider, based on configured access and secret keys
   *
   * If the keys are both set to "from-classpath", the provider will
   * look for a properties file in the classpath that contains properties
   * named access-key and secret-key.
   *
   * @param accessKey the configured accessKey, or the 'from-classpath' string
   * @param secretKey the configured secretKey, or the 'from-classpath' string
   * @return an AWSCredentialsProvider wrapping the configured keys
   */
  private[this] def createAwsCredentialsProvider(accessKey: String, secretKey: String): AWSCredentialsProvider = {

    def isClasspath(key: String) = "from-classpath".equals(key)

    if (isClasspath(accessKey) && isClasspath(secretKey)) {
      new ClasspathPropertiesFileCredentialsProvider()
    } else if (isClasspath(accessKey) || isClasspath(secretKey)) {
      throw new RuntimeException("Both access-key and secret-key must be 'from-classpath' or neither.")
    } else new AWSCredentialsProvider {
      override def getCredentials: AWSCredentials = new BasicAWSCredentials(accessKey, secretKey)
      override def refresh(): Unit = {}
    }
  }

  /**
   * Create an AmazonWebServiceConfig from the given Config
   *
   * @param config a Config object
   * @return       an instance of AmazonWebServiceConfig
   */
  private[this] def makeAmazonWebServiceConfig(config: Config) = new AmazonWebServiceConfig {
      override def endpoint: String = config.getString("endpoint")
      override def service: String = config.getString("service")
      override def region: String = config.getString("region")
    }
}
