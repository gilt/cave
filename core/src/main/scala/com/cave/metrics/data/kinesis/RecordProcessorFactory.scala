package com.cave.metrics.data.kinesis

import com.cave.metrics.data.{DataSink, AwsConfig}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorFactory}

class RecordProcessorFactory(awsConfig: AwsConfig, dataSink: DataSink) extends IRecordProcessorFactory {

  override def createProcessor: IRecordProcessor = new RecordProcessor(awsConfig, dataSink)
}
