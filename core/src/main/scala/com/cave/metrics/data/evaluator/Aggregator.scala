package com.cave.metrics.data.evaluator

import java.util.NoSuchElementException

object Aggregator extends Enumeration {
  type Aggregator = Value

  def toInflux(aggregator: Aggregator) = {
    aggregator match {
      case `p99` => "percentile(value, 99)"
      case `p999` => "percentile(value, 99.9)"
      case `p95` => "percentile(value, 95)"
      case `p90` => "percentile(value, 90)"
      case x => x + "(value)"
    }
  }

  val count, min, max, mean, mode, median, sum, stddev, p99, p999, p95, p90 = Value

  def withNameOpt(name: String): Option[Value] = {
    try {
      Some(withName(name))
    } catch {
      case _: NoSuchElementException => None
    }
  }
}

