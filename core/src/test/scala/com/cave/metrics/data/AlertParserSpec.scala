package com.cave.metrics.data

import com.cave.metrics.data.evaluator.{Aggregator, AlertParser}
import org.joda.time.LocalTime

import scala.concurrent.duration._
import org.scalatest._

class AlertParserSpec extends FlatSpec with Matchers with AlertParser with AlertJsonData {

  "an aggregator" should "generate proper sql" in {
    Aggregator.toInflux(Aggregator.count) should be("count(value)")
    Aggregator.toInflux(Aggregator.min) should be("min(value)")
    Aggregator.toInflux(Aggregator.max) should be("max(value)")
    Aggregator.toInflux(Aggregator.mean) should be("mean(value)")
    Aggregator.toInflux(Aggregator.mode) should be("mode(value)")
    Aggregator.toInflux(Aggregator.median) should be("median(value)")
    Aggregator.toInflux(Aggregator.sum) should be("sum(value)")
    Aggregator.toInflux(Aggregator.stddev) should be("stddev(value)")

    Aggregator.toInflux(Aggregator.p90) should be("percentile(value, 90)")
    Aggregator.toInflux(Aggregator.p95) should be("percentile(value, 95)")
    Aggregator.toInflux(Aggregator.p99) should be("percentile(value, 99)")
    Aggregator.toInflux(Aggregator.p999) should be("percentile(value, 99.9)")
  }

  "A metric" should "be parsed without tags" in {
    parseAll(metricSource, "orders") match {
      case Success(source, _) =>
        verifyMetric(source, "orders")

      case NoSuccess(message, _) => fail(message)
    }
  }

  it should "be parsed with a multiplication factor on the right" in {
    parseAll(anySource, "orders[] * 1.2") match {
      case Success(FactoredSource(MetricSource(metric, tags), factor), _) =>
        factor should be(1.2)
        metric should be("orders")
        tags.size should be(0)

      case _ => fail(s"Expected a metric to be parsed.")
    }
  }

  it should "be parsed with a multiplication factor on the left" in {
    parseAll(anySource, "0.7 * orders") match {
      case Success(FactoredSource(MetricSource(metric, tags), factor), _) =>
        factor should be(0.7)
        metric should be("orders")
        tags.size should be(0)

      case _ => fail(s"Expected a metric to be parsed.")
    }
  }

  it should "be parsed with one tag" in {
    parseAll(metricSource, "orders [shipCountry: US]") match {
      case Success(source, _) =>
        verifyMetric(source, "orders", "shipCountry" -> "US")

      case NoSuccess(message, _) => fail(message)
    }
  }

  it should "be parsed with multiple tags" in {
    parseAll(metricSource, ResponseTimeSvc17) match {
      case Success(source, _) =>
        verifyMetric(source,
          "response-time",
          "service" -> "svc-sale-selector",
          "env" -> "production",
          "host" -> "svc17.prod.iad")

      case NoSuccess(message, _) => fail(message)
    }
  }

  it should "not be parsed if illegal characters are used" in {
    parseAll(metricSource, "responseTime(wrong: tags)") match {
      case Success(_, _) => fail("It should not have been parsed")
      case NoSuccess(message, _) => message should be("string matching regex `\\z' expected but `(' found")
    }
  }

  "An aggregated metric" should "be parsed" in {
    parseAll(aggregatedSource, "orders[shipCountry: International].sum.15m") match {
      case Success(source, _) =>
        verifyMetric(source.metricSource, "orders", "shipCountry" -> "International")
        source.aggregator should be(Aggregator.sum)
        source.duration should be(15.minutes)

      case NoSuccess(message, _) => fail(message)
    }
  }

  it should "be parsed without tags" in {
    parseAll(aggregatedSource, "response.time[].p99.5m") match {
      case Success(source, _) =>
        verifyMetric(source.metricSource, "response.time")
        source.aggregator should be(Aggregator.p99)
        source.duration should be(5.minutes)

      case NoSuccess(message, _) => fail(message)
    }
  }

  it should "be parsed with underscores" in {
    val metric = "irishub.items.mismatched_quantity"
    parseAll(aggregatedSource, s"$metric[].p99.5m") match {
      case Success(AggregatedSource(metricSource, aggregator, duration), _) =>
        verifyMetric(metricSource, metric)
        aggregator should be(Aggregator.p99)
        duration should be(5.minutes)

      case _ => fail("Expected an aggregated metric.")
    }
  }

  "An alert" should "be parsed from source and value" in {
    parseAll(anyAlert, "orders [shipCountry: US].sum.12m <= 5 at least 3 times") match {
      case Success(SimpleAlert(left, op, right, repeats, delay), _) =>
        left match {
          case AggregatedSource(metricSource, aggregator, duration) =>
            verifyMetric(metricSource, "orders", "shipCountry" -> "US")
            aggregator should be(Aggregator.sum)
            duration should be(12.minutes)

          case _ => fail("Expected an aggregated metric source.")
        }

        op should be(Operator.LessThanOrEqual)

        right match {
          case ValueSource(num) =>
            num should be(5.0)

          case _ => fail("Expected a numeric value.")
        }

        repeats should be(3)
        delay should be(0.minutes)

      case NoSuccess(message, _) => fail(message)
    }
  }

  it should "be parsed from a complicated combination of metrics and factors" in {
    parseAll(anyAlert, "orders [shipCountry: US].sum.12m * 1.7 <= 3 * order.predHi.US at least 3 times delayed by 5m") match {
      case Success(SimpleAlert(FactoredSource(AggregatedSource(metricLeft, aggregatorLeft, durationLeft), factorLeft), op, FactoredSource(MetricSource(metricRight, tagsRight), factorRight), repeats, delay), _) =>

        verifyMetric(metricLeft, "orders", "shipCountry" -> "US")
        aggregatorLeft should be(Aggregator.sum)
        durationLeft should be(12.minutes)
        factorLeft should be(1.7)

        op should be(Operator.LessThanOrEqual)

        metricRight should be ("order.predHi.US")
        tagsRight.size should be(0)
        factorRight should be (3.0)

        repeats should be(3)
        delay should be(5.minutes)

      case _ => fail("Expected an alert to be parsed.")
    }
  }

  it should "be parsed with factored value" in {
    parseAll(anyAlert, "orders [shipCountry: US].sum.12m * 1.7 <= 3 * 1000 at least 3 times delayed by 5m") match {
      case Success(SimpleAlert(FactoredSource(AggregatedSource(metricLeft, aggregatorLeft, durationLeft), factorLeft), op, FactoredSource(ValueSource(num), factorRight), repeats, delay), _) =>

        verifyMetric(metricLeft, "orders", "shipCountry" -> "US")
        aggregatorLeft should be(Aggregator.sum)
        durationLeft should be(12.minutes)
        factorLeft should be(1.7)

        op should be(Operator.LessThanOrEqual)

        num should be(3)
        factorRight should be (1000.0)

        repeats should be(3)
        delay should be(5.minutes)

      case _ => fail("Expected an alert to be parsed.")
    }
  }

  it should "be parsed for negative amounts" in {
    parseAll(anyAlert, s"something > -100") match {
      case Success(alarm, _) =>
        alarm match {
          case SimpleAlert(left, op, right, repeats, delay) =>
            left match {
              case MetricSource(name, tags) =>
                name should be("something")
                tags.size should be(0)

              case _ => fail("Expected a simple metric source.")
            }

            op should be(Operator.GreaterThan)
            right match {
              case ValueSource(num) =>
                num should be(-100)

              case _ => fail("Expected a value source.")
            }

            repeats should be(1)
            delay should be(0.minutes)

          case _ => fail("Expected a simple alert.")
        }

      case NoSuccess(msg, _) => fail(s"Expected a simple alert, but failed: $msg")
    }
  }

  it should "be parsed from two sources" in {
    parseAll(anyAlert, s"$ResponseTime5Mp99 < $ResponseTimeDailyAvg") match {
      case Success(alarm, _) =>
        alarm match {
          case SimpleAlert(left, op, right, repeats, delay) =>
            left match {
              case AggregatedSource(metricSource, aggregator, duration) =>
                verifyMetric(metricSource, "response-time", "service" -> "svc-important", "env" -> "production")
                aggregator should be(Aggregator.p99)
                duration should be(5.minutes)

              case _ => fail("Expected an aggregated source on the left hand side.")
            }

            right match {
              case AggregatedSource(metricSource, aggregator, duration) =>
                verifyMetric(metricSource, "response-time", "service" -> "svc-important", "env" ->"production")
                aggregator should be(Aggregator.mean)
                duration should be(1.day)

              case _ => fail("Expected an aggregated source on the right hand side.")
            }

            op should be(Operator.LessThan)

            repeats should be(1)
            delay should be(0.minutes)

          case _ => fail("Expected a simple alarm")
        }
      case _ => fail("Expected a simple alarm")
    }
  }

  it should "be parsed from missing data alert" in {
    parseAll(anyAlert, s"heartbeat[svc: important, env: production] missing for 5m") match {
      case Success(missingDataAlert, _) =>
        missingDataAlert match {
          case MissingDataAlert(metricSource, duration) =>

            verifyMetric(metricSource, "heartbeat", "svc" -> "important", "env" -> "production")
            duration should be(5.minutes)

          case _ => fail("Expected a missing data alert")
        }

      case _ => fail("Expected a missing data alert")
    }
  }

  it should "be parsed from simple alert with delay" in {
    parseAll(anyAlert, s"response-time[].p99.5m < 500 delayed by 15m") match {
      case Success(alert, _) =>
        alert match {
          case SimpleAlert(left, op, right, repeats, delay) =>
            left match {
              case AggregatedSource(metricSource, aggregator, duration) =>
                verifyMetric(metricSource, "response-time")
                aggregator should be(Aggregator.p99)
                duration should be(5.minutes)

              case _ => fail("Expected an aggregated source on the left side.")
            }

            right match {
              case ValueSource(v) =>
                v should be(500.0)

              case _ => fail("Expected a fixed value on the right side.")
            }

            op should be(Operator.LessThan)
            repeats should be(1)
            delay should be(15.minutes)

          case _ => fail("Expected a simple alert.")
        }

      case _ => fail("Expected an alert to be parsed.")
    }
  }

  it should "be parsed from simple alert with repeater and delay" in {
    parseAll(anyAlert, s"response-time[].p99.5m < 500 at least 2 times delayed by 15m") match {
      case Success(alert, _) =>
        alert match {
          case SimpleAlert(left, op, right, repeats, delay) =>
            left match {
              case AggregatedSource(metricSource, aggregator, duration) =>
                verifyMetric(metricSource, "response-time")
                aggregator should be(Aggregator.p99)
                duration should be(5.minutes)

              case _ => fail("Expected an aggregated source on the left side.")
            }

            right match {
              case ValueSource(v) =>
                v should be(500.0)

              case _ => fail("Expected a fixed value on the right side.")
            }

            op should be(Operator.LessThan)
            repeats should be(2)
            delay should be(15.minutes)

          case _ => fail("Expected a simple alert.")
        }

      case _ => fail("Expected an alert to be parsed.")
    }
  }

  private def verifyMetric(actual: MetricSource, expectedName: String, expectedTags: (String, String)*) = {
    actual.metric should be(expectedName)
    verifyTags(actual.tags, expectedTags: _*)
  }

  private def verifyTags(actual: Map[String, String], expected: (String, String)*) = {
    actual.size should be(expected.size)

    for (expectedTag <- expected) {
      actual(expectedTag._1) should be(expectedTag._2)
    }
  }

  "A LocalTime" should "be parsed from a string with only hours" in {
    parseAll(daily, "@14") match {
      case Success(timeOfDay, _) =>
        timeOfDay.getHourOfDay should be (14)
        timeOfDay.getMinuteOfHour should be (0)
        timeOfDay.getSecondOfMinute should be (0)
        timeOfDay.getMillisOfSecond should be (0)

      case _ => fail("Expected a LocalTime to be parsed")
    }
  }

  it should "be parsed from a string with hours and minutes" in {
    parseAll(daily, "@14:25") match {
      case Success(timeOfDay, _) =>
        timeOfDay.getHourOfDay should be (14)
        timeOfDay.getMinuteOfHour should be (25)
        timeOfDay.getSecondOfMinute should be (0)
        timeOfDay.getMillisOfSecond should be (0)

      case _ => fail("Expected a LocalTime to be parsed")
    }
  }

  it should "be parsed from a string with hours, minutes and seconds" in {
    parseAll(daily, "@14:25:36") match {
      case Success(timeOfDay, _) =>
        timeOfDay.getHourOfDay should be (14)
        timeOfDay.getMinuteOfHour should be (25)
        timeOfDay.getSecondOfMinute should be (36)
        timeOfDay.getMillisOfSecond should be (0)

      case _ => fail("Expected a LocalTime to be parsed")
    }
  }

  "An alert period" should "be specified as a duration" in {
    parseAll(anyPeriod, "1m") match {
      case Success(duration, _) =>
        duration should be (1.minutes)

      case _ => fail("Expected a duration to be parsed")
    }
  }

  it should "be parsed as a fixed time as well" in {
    parseAll(anyPeriod, "@13:00") match {
      case Success(period, _) =>
        period shouldBe a[LocalTime]
        val localTime = period.asInstanceOf[LocalTime]
        localTime.getHourOfDay should be (13)
        localTime.getMinuteOfHour should be (0)
        localTime.getSecondOfMinute should be (0)
        localTime.getMillisOfSecond should be (0)

      case _ => fail("Expected a LocalTime to be parsed")
    }
  }
}
