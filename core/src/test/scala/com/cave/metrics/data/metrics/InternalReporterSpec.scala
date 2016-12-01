package com.cave.metrics.data.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, FlatSpec}
import org.mockito.Mockito._
import com.cave.metrics.data.{Metric => CaveMetric}

class InternalReporterSpec extends FlatSpec with Matchers with MockitoSugar {

  val publisher = mock[Iterable[CaveMetric] => Unit]
  val registry = mock[MetricRegistry]
  val clock = mock[Clock]
  val defaultTags = Map("some" -> "thing")
  val reporter = new InternalReporter(publisher, None, defaultTags, clock, registry, "test", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS)
  val epoch = 12345678L

  "gaugeToCaveMetric" should "create a metric from a numeric gauge" in {
    val gauge = mock[Gauge[Int]]
    when(gauge.getValue) thenReturn 1

    reporter.gaugeToCaveMetric(epoch)("gauge", gauge) should be(
      Some(CaveMetric("gauge.value", epoch, 1.0, defaultTags)))
  }

  it should "create a metric with tags from a properly named gauge" in {
    val gauge = mock[Gauge[Int]]
    when(gauge.getValue) thenReturn 1

    reporter.gaugeToCaveMetric(epoch)("gauge|ship=US|foo=bar", gauge) should be(
      Some(CaveMetric("gauge.value", epoch, 1.0, Map("ship" -> "US", "foo" -> "bar") ++ defaultTags)))
  }

  it should "create a metric from a byte gauge" in {
    val gauge = mock[Gauge[Byte]]
    when(gauge.getValue) thenReturn 1.toByte

    reporter.gaugeToCaveMetric(epoch)("gauge", gauge) should be(
      Some(CaveMetric("gauge.value", epoch, 1.0, defaultTags)))
  }

  it should "create a metric from a boolean gauge, mapping true to 1.0" in {
    val gauge = mock[Gauge[Boolean]]
    when(gauge.getValue) thenReturn true

    reporter.gaugeToCaveMetric(epoch)("gauge", gauge) should be(
      Some(CaveMetric("gauge.value", epoch, 1.0, defaultTags)))
  }

  it should "create a metric from a boolean gauge, mapping false to 0.0" in {
    val gauge = mock[Gauge[Boolean]]
    when(gauge.getValue) thenReturn false

    reporter.gaugeToCaveMetric(epoch)("gauge", gauge) should be(Some(CaveMetric("gauge.value", epoch, 0.0, defaultTags)))
  }

  it should "ignore string gauges" in {
    val gauge = mock[Gauge[String]]
    when(gauge.getValue) thenReturn "1.0"

    reporter.gaugeToCaveMetric(epoch)("gauge", gauge) should be(None)
  }

  "counterToCaveMetric" should "create a metric from a counter" in {
    val counter = mock[Counter]
    when(counter.getCount) thenReturn 2

    reporter.counterToCaveMetric(epoch)("counter", counter) should be(
      CaveMetric("counter.count", epoch, 2.0, defaultTags))
  }

  "histogramToCaveMetric" should "create metrics from a histogram" in {
    val histogram = mock[Histogram]
    val snapshot = mock[Snapshot]

    when(histogram.getSnapshot).thenReturn(snapshot)
    when(histogram.getCount)          thenReturn 42

    when(snapshot.getMin)             thenReturn 1
    when(snapshot.getMax)             thenReturn 2
    when(snapshot.getMean)            thenReturn 3
    when(snapshot.getStdDev)          thenReturn 4
    when(snapshot.getMedian)          thenReturn 5
    when(snapshot.get75thPercentile)  thenReturn 6
    when(snapshot.get95thPercentile)  thenReturn 7
    when(snapshot.get98thPercentile)  thenReturn 8
    when(snapshot.get99thPercentile)  thenReturn 9
    when(snapshot.get999thPercentile) thenReturn 10

    reporter.histogramToCaveMetrics(epoch)("histogram", histogram) should be(Seq(
      CaveMetric("histogram.count",  epoch, 42.0, defaultTags),
      CaveMetric("histogram.min",    epoch,  1.0, defaultTags),
      CaveMetric("histogram.max",    epoch,  2.0, defaultTags),
      CaveMetric("histogram.mean",   epoch,  3.0, defaultTags),
      CaveMetric("histogram.stddev", epoch,  4.0, defaultTags),
      CaveMetric("histogram.p50",    epoch,  5.0, defaultTags),
      CaveMetric("histogram.p75",    epoch,  6.0, defaultTags),
      CaveMetric("histogram.p95",    epoch,  7.0, defaultTags),
      CaveMetric("histogram.p98",    epoch,  8.0, defaultTags),
      CaveMetric("histogram.p99",    epoch,  9.0, defaultTags),
      CaveMetric("histogram.p999",   epoch, 10.0, defaultTags)
    ))
  }

  "meterToCaveMetric" should "create metrics from a meter" in {
    val meter = mock[Meter]
    when(meter.getCount)             thenReturn 42
    when(meter.getMeanRate)          thenReturn 1.0
    when(meter.getOneMinuteRate)     thenReturn 2.0
    when(meter.getFiveMinuteRate)    thenReturn 3.0
    when(meter.getFifteenMinuteRate) thenReturn 4.0

    reporter.meterToCaveMetrics(epoch)("meter", meter) should be(Seq(
      CaveMetric("meter.count",     epoch, 42.0, defaultTags),
      CaveMetric("meter.mean_rate", epoch,  1.0, defaultTags),
      CaveMetric("meter.m1_rate",   epoch,  2.0, defaultTags),
      CaveMetric("meter.m5_rate",   epoch,  3.0, defaultTags),
      CaveMetric("meter.m15_rate",  epoch,  4.0, defaultTags)
    ))
  }
}
