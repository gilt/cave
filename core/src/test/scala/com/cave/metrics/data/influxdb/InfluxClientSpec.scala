package com.cave.metrics.data.influxdb

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.duration._

class InfluxClientSpec extends FlatSpec with Matchers with MockitoSugar {

  val SomeData =
    """
      |[
      |  {
      |    "name": "orders",
      |    "columns": [ "time", "sequence_number", "value" ],
      |    "points": [
      |      [ 1404086940, 54800001, 0],
      |      [ 1404086880, 54820001, 0],
      |      [ 1404086820, 54850001, 0],
      |      [ 1404086760, 54860001, 0],
      |      [ 1404086700, 54890001, 0],
      |      [ 1404086640, 54900001, 0],
      |      [ 1404086580, 54930001, 0],
      |      [ 1404086520, 54940001, 0],
      |      [ 1404086460, 54960001, 0]
      |    ]
      |  }
      |]
    """.stripMargin

  val mockConfig = mock[InfluxClusterConfig]
  val influxClient = new InfluxClient(mockConfig)

  "createResponse" should "return a proper map of data points" in {
    val data = influxClient.createResponse(SomeData)

    data.metrics.size should be(9)
    data.metrics.find(_.time == new DateTime(1404086940000L)).map(_.value) should be(Some(0))
  }

  "Influx Client" should "generate valid Continuous Queries" in {
    influxClient.createSQL("METRIC",
      Map("tag_name1" -> "tag_val1", "tag_name2" -> "tag_val2"),
      "AGGREGATOR", 5.seconds , "QUERY_NAME") should be (
      """select AGGREGATOR as value from "METRIC" where tag_name1='tag_val1' and tag_name2='tag_val2' group by time(5s) into "QUERY_NAME"""")

    influxClient.createSQL("METRIC", Map(), "AGGREGATOR", 5.hours , "QUERY_NAME") should be (
      """select AGGREGATOR as value from "METRIC" group by time(18000s) into "QUERY_NAME"""")
  }

  val StartDateString = "2014-09-24 09:43:00"
  val StartDate = ISODateTimeFormat.dateTimeNoMillis().parseDateTime("2014-09-24T09:43:00Z")

  val EndDateString = "2014-09-25 09:43:00"
  val EndDate = ISODateTimeFormat.dateTimeNoMillis().parseDateTime("2014-09-25T09:43:00Z")

  it should "create valid queries for GetMetricData" in {
    influxClient.buildQuery("orders", Map.empty[String, String], None, None, None) should be(
      """select value from "orders" limit 1440""")

    influxClient.buildQuery("orders", Map.empty[String, String], None, None, Some(1)) should be(
      """select value from "orders" limit 1""")

    influxClient.buildQuery("orders", Map("shipTo" -> "US"), None, None, None) should be(
      """select value from "orders" where shipTo='US' limit 1440""")

    influxClient.buildQuery("orders", Map("shipTo" -> "US"), Some(StartDate), None, None) should be(
      s"""select value from "orders" where shipTo='US' and time > '$StartDateString' limit 1440""")

    influxClient.buildQuery("orders", Map("shipTo" -> "US"), None, Some(EndDate), None) should be(
      s"""select value from "orders" where shipTo='US' and time < '$EndDateString' limit 1440""")

    influxClient.buildQuery("orders", Map("shipTo" -> "US"), Some(StartDate), Some(EndDate), None) should be(
      s"""select value from "orders" where shipTo='US' and time > '$StartDateString' and time < '$EndDateString' limit 1440""")

    influxClient.buildQuery("orders", Map("shipTo" -> "US"), Some(StartDate), Some(EndDate), Some(2000)) should be(
      s"""select value from "orders" where shipTo='US' and time > '$StartDateString' and time < '$EndDateString' limit 1440""")

    influxClient.buildQuery("orders", Map("shipTo" -> "US"), Some(StartDate), Some(EndDate), Some(2)) should be(
      s"""select value from "orders" where shipTo='US' and time > '$StartDateString' and time < '$EndDateString' limit 2""")
  }

  it should "create valid queries for aggregated metrics" in {
    influxClient.buildAggregatedQuery("sum(value)", 5.minutes, "orders", Map.empty[String, String], None, None, None) should be(
      """select sum(value) from "orders" group by time(300s) limit 1440""")

    influxClient.buildAggregatedQuery("sum(value)", 5.minutes, "orders", Map.empty[String, String], None, None, Some(1)) should be(
      """select sum(value) from "orders" group by time(300s) limit 1""")

    influxClient.buildAggregatedQuery("sum(value)", 5.minutes, "orders", Map("shipTo" -> "US"), None, None, None) should be(
      """select sum(value) from "orders" where shipTo='US' group by time(300s) limit 1440""")

    influxClient.buildAggregatedQuery("sum(value)", 5.minutes, "orders", Map("shipTo" -> "US"), Some(StartDate), None, None) should be(
      s"""select sum(value) from "orders" where shipTo='US' and time > '$StartDateString' group by time(300s) limit 1440""")

    influxClient.buildAggregatedQuery("sum(value)", 5.minutes, "orders", Map("shipTo" -> "US"), None, Some(EndDate), None) should be(
      s"""select sum(value) from "orders" where shipTo='US' and time < '$EndDateString' group by time(300s) limit 1440""")

    influxClient.buildAggregatedQuery("sum(value)", 5.minutes, "orders", Map("shipTo" -> "US"), Some(StartDate), Some(EndDate), None) should be(
      s"""select sum(value) from "orders" where shipTo='US' and time > '$StartDateString' and time < '$EndDateString' group by time(300s) limit 1440""")

    influxClient.buildAggregatedQuery("sum(value)", 5.minutes, "orders", Map("shipTo" -> "US"), Some(StartDate), Some(EndDate), Some(2000)) should be(
      s"""select sum(value) from "orders" where shipTo='US' and time > '$StartDateString' and time < '$EndDateString' group by time(300s) limit 1440""")

    influxClient.buildAggregatedQuery("sum(value)", 5.minutes, "orders", Map("shipTo" -> "US"), Some(StartDate), Some(EndDate), Some(2)) should be(
      s"""select sum(value) from "orders" where shipTo='US' and time > '$StartDateString' and time < '$EndDateString' group by time(300s) limit 2""")
  }


  val SomeMetrics =
    """
      |[
      |  {
      |    "points": [ [ 1410878579000, 1226770002, 1, "29" ] ],
      |    "columns": [ "time", "sequence_number", "value", "alert" ],
      |    "name": "alertsHistory"
      |  },
      |  {
      |    "points": [ [ 1413890367000, 2576490001, 1, "stage57", "svc-sku-pricing", "stage" ] ],
      |    "columns": [ "time", "sequence_number", "value", "host", "service", "environment" ],
      |    "name": "svc-sku-pricing-publisher"
      |  }
      |]
    """.stripMargin

  "createMetricInfoResponse" should "return a list of MetricInfo objects" in {
    val data = influxClient.createMetricInfoResponse(SomeMetrics)

    data.size should be(2)
    val skuData = data.find(_.name == "svc-sku-pricing-publisher").get
    skuData.tags.size should be(3)
    skuData.tags.contains("environment") should be(true)
    skuData.tags.contains("service") should be(true)
    skuData.tags.contains("host") should be(true)

    val alertsHistory = data.find(_.name == "alertsHistory").get
    alertsHistory.tags.size should be(1)
    alertsHistory.tags.contains("alert") should be(true)
  }
}
