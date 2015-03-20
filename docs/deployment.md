## Deployment

Aside from the CAVE micro services, the following requirements must be met:

  * a Postgres database instance for storing configuration information. The database must be bootstrapped with [Schema Evolution Manager](https://github.com/gilt/schema-evolution-manager), please see the `schema` folder in the project for more information.
  * an InfluxDB cluster, v0.8.x. We are using a round-robin DNS to spread tge load uniformly across the cluster.
  * a SendGrid account (or something similar) for sending emails. If you have your own SMTP server, use that.
  * an Amazon Kinesis stream for raw data. This could be replaced with Kafka or similar, but there's no support for different queueing systems.
  * an Amazon SNS topic and Amazon SQS queues for sending configuration changes from API to all schedulers. This can also be replaced with other PubSub mechanisms, but there's no support for anything else.
  * subscription for Amazon CloudWatch Logs, if you require log aggregation. Otherwise, this can be turned off in the logback configuration, by removing the cloudwatch appender.
  * one Amazon SQS queue for communication between Schedulers and Workers (producer/consumer design pattern). Again, this can be replaced with other AMQP systems, but no support exists.

### CAVE Services

The following information should help you get a CAVE stack up and running.

#### Building Images
Each service can be built into a Docker image from the source code, or you can run the official Docker images from docker.io repository.

To build a single service, you can run:

```
sbt <module>/docker:publishLocal
```

where `<module>` is any of `api`, `dbwriter`, `scheduler`, `worker`, or `www`.

Depending on the shell you're using, you can build them all with a loop command. For example, in `zsh`, you can run:

```
for p (api dbwriter scheduler worker www); { sbt $p/docker:publishLocal }
```

This command will build the Docker image(s) and publish them to a local Docker repository. For example, on OS X, you can use [boot2docker](http://boot2docker.io) or [kitematic](https://kitematic.com).

#### Configuration
Each CAVE service takes its *secrets* from configuration, none are hard-coded, and all can be overridden at runtime. The service specific configuration resides under `<module>/conf/` directory.

In the following sections, you will find which configuration parameters need to be either added to the configuration files, or provided to the `docker run` command at launch time.

#### AWS Access
If you plan to use AWS to deploy CAVE, as we did, here are a few notes on how we set up access. Each CAVE service requires access to some AWS services, there are not two alike. For example, the API service and the Worker service require ability to write into the Kinesis stream, but do not need access to any other action. On the other hand, the DB Writer service is the only one that requires ability to read the stream, but doesn't need to write events into it.

What we did, and recommend you do the same, is to create IAM users for each service, without passwords, but with access keys, and with appropriate policies for access. In the following sections, where we discuss configuration for each CAVE service, we will outline required AWS access for each, with example policies that we applied.

#### API Service
This service resides in the `cavellc/cave-api` image. To get access to the required dependencies, API service requires the following configuration parameters:

  * `api-service.aws.rds.database-user`: The username for the Postgres database.
  * `api-service.aws.rds.database-password`: The password for the Postgres database.
  * `api-service.aws.credentials.access-key`: Access Key for the API Service AWS user.
  * `api-service.aws.credentials.secret-key`: Secret Key for the API Service AWS user.
  * `api-service.influx.user`: User name for the InfluxDB cluster administrator account.
  * `api-service.influx.pass`: Password for the InfluxDB cluser administrator account.
  * `smtp.user`: User name for the SendGrid account.
  * `smtp.password`: Password for the SendGrid account.
  
The API service interacts with two AWS services: it must be able to write events into the Kinesis raw data stream, and it also creates a configuration changes topic in SNS, then sends notifications about configuration changes into this topic.

We created an IAM user for this service and attached this access policy to it:

```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [ "kinesis:PutRecord" ],
      "Resource": "arn:aws:kinesis:*:<account>:stream/raw-metric-data"
    },
    {
      "Effect":"Allow",
      "Action": [ "sns:CreateTopic", "sns:SetTopicAttributes", "sns:DeleteTopic" ],
      "Resource": "arn:aws:sns:*:<account>:configuration-changes"
    }
  ]
}
```

The above policy gives the user the required abilities, and nothing else. Just replace <account> with your own AWS Account identifier.

#### DB Writer Service
This service resides in the `cavellc/cave-dbwriter` image. It requires the following configuration parameters:

  * `db-writer.aws.credentials.access-key`: Access Key for the DB Writer Service AWS user.
  * `db-writer.aws.credentials.secret-key`: Secret Key for the DB Writer Service AWS user.
  * `db-writer.influx.user`: User name for the InfluxDB cluster administrator account.
  * `db-writer.influx.pass`: Password for the InfluxDB cluser administrator account.
  
This service uses the [Kinesis Client Library](https://github.com/awslabs/amazon-kinesis-client) to connect to Kinesis and consume events from the raw data stream. Apart from read-only access to Kinesis, it also requires privileges for DynamoDB, a no-SQL database service, where it creates a table for shard allocation across a consumer fleet. It is not well documented which privileges the library needs for Dynamo, so we've given it full rights, which is not the best from a security point of view.

This is the access policy we attached to `cave-dbwriter`, the IAM user created for this service:

```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [ "kinesis:Get*", "kinesis:List*", "kinesis:Describe*" ],
      "Resource": "arn:aws:kinesis:*:<account>:stream/raw-metric-data"
    },
    {
      "Effect": "Allow",
      "Action": [ "dynamodb:*" ],
      "Resource": "*"
    }
  ]
}
```

This policy gives the user read-only access to Kinesis, and full privileges to Dynamo.

#### Scheduler Service
This service resides in the `cavellc/cave-scheduler` image. It requires these configuration parameters:

  * `scheduler.aws.rds.database-user`: The username for the Postgres database.
  * `scheduler.aws.rds.database-password`: The password for the Postgres database.
  * `scheduler.aws.credentials.access-key`: Access Key for the Scheduler Service AWS user.
  * `scheduler.aws.credentials.secret-key`: Secret Key for the Scheduler Service AWS user.
 
The Scheduler service uses SNS and SQS to keep its configuration in sync, by creating an SQS queue and subscribing it to the `configuration-changes` SNS topic where the API sends notifications. It also uses SQS to send alert schedules to the Worker instances. To create a singleton that is the master, we have all the Scheduler nodes in one Akka cluster. To seed this cluster, we use the Auto Scaling and EC2 services to find the scheduler group instances and their host names.

This is the access policy attached to the AWS user we created for this service:

```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [ "sqs:*" ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [ "sns:*" ],
      "Resource": "arn:aws:sns:*:<account>:configuration-changes"
    },
    {
      "Effect": "Allow",
      "Action": "ec2:Describe*",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": "autoscaling:Describe*",
      "Resource": "*"
    }
  ]
}
```

#### Worker Service
This service resides in the `cavellc/cave-worker` image. These are the required configuration parameters:

  * `worker.aws.credentials.access-key`: Access Key for the Worker Service AWS user.
  * `worker.aws.credentials.secret-key`: Secret Key for the Worker Service AWS user.
  * `worker.influx.user`: User name for the InfluxDB cluster administrator account.
  * `worker.influx.pass=metrics`: Password for the InfluxDB cluser administrator account.
  
This service uses SQS to pull work from the `alarm-schedule` queue, and it also needs to be able to write alert history as raw data into Kinesis. Here's the access policy for the AWS user this services employs:

```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [ "sqs:*" ],
      "Effect": "Allow",
      "Resource": "arn:aws:sqs:*:<account>:alarm-schedule"
    },
    {
      "Effect": "Allow",
      "Action": [ "kinesis:Get*", "kinesis:List*", "kinesis:Describe*", "kinesis:Put*" ],
      "Resource": "arn:aws:kinesis:*:<account>:*/raw-metric-data"
    }
  ]
}
```

