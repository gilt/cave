package com.cave.metrics.data.metrics

import java.util.concurrent.TimeUnit
import java.util.{SortedMap => JSortedMap}

import com.cave.metrics.data.{Metric => CaveMetric}
import com.codahale.metrics._

import scala.collection.JavaConversions._

case class InternalReporter(publisher: Iterable[CaveMetric] => Unit,
                       prefix: Option[String],
                       tags: Map[String, String],
                       clock: Clock,
                       registry: MetricRegistry,
                       name: String,
                       filter: MetricFilter,
                       rateUnit: TimeUnit,
                       durationUnit: TimeUnit) extends ScheduledReporter(registry, name, filter, rateUnit, durationUnit) {

  case class NameAndTags(name: String, tags: Map[String, String] = Map.empty[String, String])

  def build(nt: NameAndTags, suffix: String, timestamp: Long, value: Double): CaveMetric = {
    def makeName(name: String, suffix: String) = {
      val unPrefixedName = name + "." + suffix
      prefix.fold(unPrefixedName)(_ + "." + unPrefixedName)
    }

    CaveMetric(makeName(nt.name, suffix), timestamp, value, tags ++ nt.tags)
  }


  override def report(gauges: JSortedMap[String, Gauge[_]],
                      counters: JSortedMap[String, Counter],
                      histograms: JSortedMap[String, Histogram],
                      meters: JSortedMap[String, Meter],
                      timers: JSortedMap[String, Timer]): Unit = {

    try {
      val epoch = clock.getTime / 1000


      val caveMetrics: Iterable[CaveMetric] =
        gauges.map ((gaugeToCaveMetric(epoch) _).tupled).flatten ++
        counters.map ((counterToCaveMetric(epoch) _).tupled) ++
        histograms.flatMap ((histogramToCaveMetrics(epoch) _).tupled) ++
        meters.flatMap ((meterToCaveMetrics(epoch) _).tupled) ++
        timers.flatMap ((timerToCaveMetric(epoch) _).tupled)

      publisher(caveMetrics)
    }
  }

  private[metrics] def gaugeToCaveMetric(timestamp: Long)(name: String, gauge: Gauge[_]): Option[CaveMetric] = {
    def makeMetric(value: Double) = Some(build(parseNameAndTags(name), "value", timestamp, value))
    gauge.getValue match {
      case n: Number => makeMetric(n.doubleValue())
      case b: Byte => makeMetric(b.toDouble)
      case b: Boolean => makeMetric(if (b) 1.0 else 0.0)
      case _ => None
    }
  }

  private[metrics] def counterToCaveMetric(timestamp: Long)(name: String, counter: Counter): CaveMetric =
    build(parseNameAndTags(name), "count", timestamp, counter.getCount)

  private[metrics] def histogramToCaveMetrics(timestamp: Long)(name: String, histogram: Histogram): Seq[CaveMetric] = {
    val nt = parseNameAndTags(name)

    val snapshot = histogram.getSnapshot
    Seq(
      build(nt, "count",  timestamp, histogram.getCount),
      build(nt, "min",    timestamp, snapshot.getMin),
      build(nt, "max",    timestamp, snapshot.getMax),
      build(nt, "mean",   timestamp, snapshot.getMean),
      build(nt, "stddev", timestamp, snapshot.getStdDev),
      build(nt, "p50",    timestamp, snapshot.getMedian),
      build(nt, "p75",    timestamp, snapshot.get75thPercentile),
      build(nt, "p95",    timestamp, snapshot.get95thPercentile),
      build(nt, "p98",    timestamp, snapshot.get98thPercentile),
      build(nt, "p99",    timestamp, snapshot.get99thPercentile),
      build(nt, "p999",   timestamp, snapshot.get999thPercentile)
    )
  }

  private[metrics] def meterToCaveMetrics(timestamp: Long)(name: String, meter: Metered): Seq[CaveMetric] = {
    val nt = parseNameAndTags(name)

    Seq(
      build(nt, "count", timestamp, meter.getCount),
      build(nt, "mean_rate", timestamp, convertRate(meter.getMeanRate)),
      build(nt, "m1_rate", timestamp, convertRate(meter.getOneMinuteRate)),
      build(nt, "m5_rate", timestamp, convertRate(meter.getFiveMinuteRate)),
      build(nt, "m15_rate", timestamp, convertRate(meter.getFifteenMinuteRate))
    )
  }

  private[metrics] def timerToCaveMetric(timestamp: Long)(name: String, timer: Timer): Seq[CaveMetric] = {
    val nt = parseNameAndTags(name)

    val snapshot = timer.getSnapshot
    meterToCaveMetrics(timestamp)(name, timer) ++ Seq(
      build(nt, "min", timestamp, convertDuration(snapshot.getMin)),
      build(nt, "max", timestamp, convertDuration(snapshot.getMax)),
      build(nt, "mean", timestamp, convertDuration(snapshot.getMean)),
      build(nt, "stddev", timestamp, convertDuration(snapshot.getStdDev)),
      build(nt, "p50", timestamp, convertDuration(snapshot.getMedian)),
      build(nt, "p75", timestamp, convertDuration(snapshot.get75thPercentile)),
      build(nt, "p95", timestamp, convertDuration(snapshot.get95thPercentile)),
      build(nt, "p98", timestamp, convertDuration(snapshot.get98thPercentile)),
      build(nt, "p99", timestamp, convertDuration(snapshot.get99thPercentile)),
      build(nt, "p999", timestamp, convertDuration(snapshot.get999thPercentile))
    )
  }

  private[metrics] def parseNameAndTags(name: String): NameAndTags = {
    name.split("[|]").toList match {
      case h::Nil => NameAndTags(name, Map.empty[String, String])
      case h::t => NameAndTags(h, splitTags(t))
      case _ => sys.error("This should never happen.")
    }
  }

  private[metrics] def splitTags(fields: List[String]): Map[String, String] = {
    val pairs = for {
      k :: v :: Nil <- fields.map(_.split('=').toList)
    } yield k -> v

    pairs.toMap
  }
}

object InternalReporter {
  val DefaultTags = Map("host" -> java.net.InetAddress.getLocalHost.getCanonicalHostName)

  def apply(prefix: Option[String] = None,
            tags: Map[String, String] = DefaultTags,
            clock: Clock = Clock.defaultClock(),
            registry: MetricRegistry,
            name: String = "internal",
            filter: MetricFilter = MetricFilter.ALL,
            rateUnit: TimeUnit = TimeUnit.SECONDS,
            durationUnit: TimeUnit = TimeUnit.MILLISECONDS)(publisher: Iterable[CaveMetric] => Unit) =
    new InternalReporter(publisher, prefix, tags, clock, registry, name, filter, rateUnit, durationUnit)
}
