package com.cave.metrics.data

import com.cave.metrics.data.evaluator.Aggregator
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import org.scalatest._
import play.api.libs.json.Json
import scala.concurrent.duration._

import scala.util.Try

class MetricSpec extends FlatSpec with Matchers with MetricJsonData {

  "Metric" should "be parsed from simple json" in {
    val metric = Json.parse(ValidMetricOrdersUS).as[Metric]

    metric should have(
      'name("orders"),
      'timestamp(1395915330),
      'value(10),
      'tags(Map("shipCountry" -> "US"))
    )
  }

  it should "not be parsed from bad json" in {
    Try {
      Json.parse(BadMetricJson).as[Metric]
      fail("exception not thrown")
    }
    Try {
      Json.parse(OrdersUSMissingValue).as[Metric]
      fail("exception not thrown")
    }
    Try {
      Json.parse(OrdersCAMissingTimestamp).as[Metric]
      fail("exception not thrown")
    }
  }

  it should "be valid if it has short enough fields and not too many tags" in {
    Metric("orders", 1395915330, 10, Map("shipCountry" -> "US")).isSane should be(true)
  }

  final val VeryLongString =
    """This is a very long name for a metric,
      |exceeding the limit we imposed on such a thing,
      |so that we protect the service from potential abuse
      |caused by spammers with very long data fields""".stripMargin

  it should "be invalid if it has a name too long" in {
    Metric(VeryLongString,
      1395915330, 10, Map("shipCountry" -> "US")).isSane should be(false)
  }

  it should "be invalid if it has too many tags" in {
    Metric("orders",
      1395915330, 10,
      Map(
        "shipCountry" -> "US",
        "this" -> "this",
        "that" -> "that",
        "and" -> "and",
        "another" -> "another",
        "tag" -> "tag",
        "which" -> "which",
        "puts" -> "puts",
        "us" -> "us",
        "over" -> "over",
        "the" -> "the",
        "limit" -> "limit"
      )).isSane should be(false)
  }

  it should "be invalid if it has a tag name too long" in {
    Metric("orders", 1395915330, 10, Map(VeryLongString -> "US")).isSane should be(false)
  }

  it should "be invalid if it has a tag value too long" in {
    Metric("orders", 1395915330, 10, Map("shipCountry" -> VeryLongString)).isSane should be(false)
  }

  "Metric List" should "be parsed from json array" in {

    val maybeMetrics = Json.parse(ValidMetrics).as[MetricBulk].metrics
    maybeMetrics should not be None


    maybeMetrics(0).name should be("orders")
    maybeMetrics(0).timestamp should be(1395915330)
    maybeMetrics(0).value should be(10)
    maybeMetrics(0).tags should be(Map("shipCountry" -> "US"))

    maybeMetrics(1).name should be("orders")
    maybeMetrics(1).timestamp should be(1395915330)
    maybeMetrics(1).value should be(2)
    maybeMetrics(1).tags should be(Map("shipCountry" -> "CA"))
  }

  private val MetricName = "orders"

  private val TagOneKey = "shipTo"
  private val TagOneValue = "US"
  private val TagTwoKey = "env"
  private val TagTwoValue = "prod"
  private val TagsOne = s"$TagOneKey:$TagOneValue"
  private val TagsTwo = s"$TagsOne,$TagTwoKey:$TagTwoValue"

  private val Start = "2014-09-23T13:58:00.000Z"
  private val End = "2014-09-23T14:58:00.000Z"
  private val Limit = "12"
  private val LimitNum = 12

  private val Agg = Aggregator.sum
  private val AggString = Agg.toString
  private val BadAggString = "boom"
  private val Period = "1"
  private val PeriodNum = 1
  private val BadPeriod = "boom"

  "MetricRequest" should "be built from query string" in {
    val params = Map(
      "metric" -> Seq(MetricName),
      "tags" -> Seq(TagsTwo),
      "start" -> Seq(Start),
      "end" -> Seq(End),
      "limit" -> Seq(Limit),
      "aggregator" -> Seq(AggString),
      "period" -> Seq(Period)
    )

    val result = MetricRequest.fromQueryString(params)
    result.isSuccess should be (true)

    val request = result.get
    request.metric should be (MetricName)

    request.tags.size should be (2)
    request.tags.get(TagOneKey) should be(Some(TagOneValue))
    request.tags.get(TagTwoKey) should be(Some(TagTwoValue))

    request.start.isDefined should be(true)
    request.start.get.withZone(DateTimeZone.UTC).toString(ISODateTimeFormat.dateTime()) should be (Start)

    request.end.isDefined should be(true)
    request.end.get.withZone(DateTimeZone.UTC).toString(ISODateTimeFormat.dateTime()) should be (End)

    request.limit should be(Some(LimitNum))

    request.aggregator should be(Agg)
    request.period should be(PeriodNum.minutes)
  }

  it should "be built from query string with optional parts missing" in {
    val params = Map(
      "metric" -> Seq(MetricName),
      "aggregator" -> Seq(AggString),
      "period" -> Seq(Period)
    )

    val result = MetricRequest.fromQueryString(params)
    result.isSuccess should be (true)

    val request = result.get
    request.metric should be (MetricName)
    request.aggregator should be(Agg)
    request.period should be(PeriodNum.minutes)

    request.tags.size should be (0)

    request.start.isDefined should be(false)
    request.end.isDefined should be(false)
    request.limit should be(None)
  }

  it should "fail to build from query string when metric is missing" in {
    val params = Map(
      "tags" -> Seq(TagsTwo),
      "start" -> Seq(Start),
      "end" -> Seq(End),
      "limit" -> Seq(Limit),
      "aggregator" -> Seq(AggString),
      "period" -> Seq(Period)
    )

    val result = MetricRequest.fromQueryString(params)
    result.isFailure should be (true)
  }

  it should "fail to build from query string when aggregator is missing" in {
    val params = Map(
      "metric" -> Seq(MetricName),
      "tags" -> Seq(TagsTwo),
      "start" -> Seq(Start),
      "end" -> Seq(End),
      "limit" -> Seq(Limit),
      "period" -> Seq(Period)
    )

    val result = MetricRequest.fromQueryString(params)
    result.isFailure should be (true)
  }

  it should "fail to build from query string when aggregator is invalid" in {
    val params = Map(
      "metric" -> Seq(MetricName),
      "tags" -> Seq(TagsTwo),
      "start" -> Seq(Start),
      "end" -> Seq(End),
      "limit" -> Seq(Limit),
      "aggregator" -> Seq(BadAggString),
      "period" -> Seq(Period)
    )

    val result = MetricRequest.fromQueryString(params)
    result.isFailure should be (true)
  }

  it should "fail to build from query string when period is missing" in {
    val params = Map(
      "metric" -> Seq(MetricName),
      "tags" -> Seq(TagsTwo),
      "start" -> Seq(Start),
      "end" -> Seq(End),
      "limit" -> Seq(Limit),
      "aggregator" -> Seq(AggString)
    )

    val result = MetricRequest.fromQueryString(params)
    result.isFailure should be (true)
  }

  it should "fail to build from query string when period is invalid" in {
    val params = Map(
      "metric" -> Seq(MetricName),
      "tags" -> Seq(TagsTwo),
      "start" -> Seq(Start),
      "end" -> Seq(End),
      "limit" -> Seq(Limit),
      "aggregator" -> Seq(BadPeriod),
      "period" -> Seq(Period)
    )

    val result = MetricRequest.fromQueryString(params)
    result.isFailure should be (true)
  }
}
