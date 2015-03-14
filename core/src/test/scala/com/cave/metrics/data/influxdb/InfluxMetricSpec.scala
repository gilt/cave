package com.cave.metrics.data.influxdb

import com.cave.metrics.data.Metric
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class InfluxMetricSpec extends FlatSpec with Matchers with MockitoSugar {

  private val METRIC_NAME = "orders"
  private val TIMESTAMP = 1L
  private val VALUE = 1.0

  private val ORG1_NAME = "org1"
  private val ORG2_NAME = "org2"

  private def withAccountTag(orgName: String, tags: Map[String, String]) = tags ++ Map(Metric.Organization -> orgName)

  private val NO_TAGS = Map.empty[String, String]

  private val TAG1_KEY = "shipTo"
  private val TAG1_VALUE = "US"
  private val ONE_TAG = Map(TAG1_KEY -> TAG1_VALUE)

  private val TAG2_KEY = "service"
  private val TAG2_VALUE = "svc-important"
  private val TWO_TAGS = Map(TAG1_KEY -> TAG1_VALUE, TAG2_KEY -> TAG2_VALUE)

  "an InfluxMetric" should "be created from a CAVE metric without tags" in {
    val result = InfluxMetric.prepareRequests(Seq(Metric(METRIC_NAME, TIMESTAMP, VALUE, withAccountTag(ORG1_NAME, NO_TAGS))))

    result.size should be(1)
    val org = result.get(ORG1_NAME)
    org should be(Some(s"""[{"name":"$METRIC_NAME","columns":["time","value"],"points":[[$TIMESTAMP,$VALUE]]}]"""))
  }

  it should "be created from a CAVE metric with one tag" in {
    val result = InfluxMetric.prepareRequests(Seq(Metric(METRIC_NAME, TIMESTAMP, VALUE, withAccountTag(ORG1_NAME, ONE_TAG))))

    result.size should be(1)
    val org = result.get(ORG1_NAME)
    org should be(Some(s"""[{"name":"$METRIC_NAME","columns":["time","value","$TAG1_KEY"],"points":[[$TIMESTAMP,$VALUE,"$TAG1_VALUE"]]}]"""))
  }

  it should "be created from a CAVE metric with two tags" in {
    val result = InfluxMetric.prepareRequests(Seq(Metric(METRIC_NAME, TIMESTAMP, VALUE, withAccountTag(ORG1_NAME, TWO_TAGS))))

    result.size should be(1)
    val org = result.get(ORG1_NAME)
    org should be(Some(s"""[{"name":"$METRIC_NAME","columns":["time","value","$TAG1_KEY","$TAG2_KEY"],"points":[[$TIMESTAMP,$VALUE,"$TAG1_VALUE","$TAG2_VALUE"]]}]"""))
  }

  it should "be separated by org name" in {
    val result = InfluxMetric.prepareRequests(Seq(
      Metric(METRIC_NAME, TIMESTAMP, VALUE, withAccountTag(ORG1_NAME, NO_TAGS)),
      Metric(METRIC_NAME, TIMESTAMP, VALUE, withAccountTag(ORG2_NAME, ONE_TAG))
    ))

    result.size should be(2)

    result.get(ORG1_NAME) should be(Some(s"""[{"name":"$METRIC_NAME","columns":["time","value"],"points":[[$TIMESTAMP,$VALUE]]}]"""))
    result.get(ORG2_NAME) should be(Some(s"""[{"name":"$METRIC_NAME","columns":["time","value","$TAG1_KEY"],"points":[[$TIMESTAMP,$VALUE,"$TAG1_VALUE"]]}]"""))
  }

  it should "not be parsed if the Organization tag is missing" in {
    try {
      InfluxMetric.prepareRequests(Seq(Metric(METRIC_NAME, TIMESTAMP, VALUE, ONE_TAG)))
      fail("Expected to throw an exception")
    } catch {
      case e: RuntimeException =>
        e.getMessage should be(InfluxMetric.MissingAccountTagMessage)
    }
  }
}
