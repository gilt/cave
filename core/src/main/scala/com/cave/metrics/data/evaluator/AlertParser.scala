package com.cave.metrics.data.evaluator

import java.security.MessageDigest

import org.joda.time.LocalTime

import scala.concurrent.duration._
import scala.util.parsing.combinator.JavaTokenParsers


trait AlertParser extends JavaTokenParsers {

  def anyAggregator = (
      for {
        value <- Aggregator.values
      } yield value.toString: Parser[String]
    ).reduce(_ | _)

  object Operator extends Enumeration {
    type Operator = Value
    val LessThan, LessThanOrEqual, GreaterThan, GreaterThanOrEqual, Equal, NotEqual = Value

    private val symbols = Seq(
      LessThanOrEqual -> "<=",
      LessThan -> "<",
      GreaterThanOrEqual -> ">=",
      GreaterThan -> ">",
      Equal -> "==",
      NotEqual -> "!="
    )

    def byName(name: String): Operator =
      symbols.find(_._2 == name).map(_._1).getOrElse(sys.error(s"Unknown symbol $name"))

    def anyValue = symbols.map(f => f._2: Parser[String]).reduce(_ | _)
  }

  import Operator._
  import com.cave.metrics.data.evaluator.Aggregator._

  sealed trait Source

  case class ValueSource(value: Double) extends Source

  case class MetricSource(metric: String, tags: Map[String, String]) extends Source {
    override def toString = metric + tags.toSeq.sortBy(_._1).map { case (key, value) => key + "." + value}.mkString("__", ".", "")
  }

  case class AggregatedSource(metricSource: MetricSource, aggregator: Aggregator, duration: FiniteDuration) extends Source {
    override def toString: String = {
      val key = metricSource + "__" + aggregator.toString + "__" + duration.toSeconds
      val md = MessageDigest.getInstance("SHA-512")
      md.update(key.getBytes())
      md.digest().map("%02x" format _).mkString
    }
  }

  case class FactoredSource(source: Source, factor: Double) extends Source

  sealed trait AlertEntity

  case class SimpleAlert(sourceLeft: Source, operator: Operator, sourceRight: Source, times: Int, delay: FiniteDuration) extends AlertEntity
  case class MissingDataAlert(metricSource: MetricSource, duration: FiniteDuration) extends AlertEntity


  /** *** Parsers *****/
  def operator: Parser[Operator] = Operator.anyValue ^^ Operator.byName

  def aggregator: Parser[Aggregator] = anyAggregator ^^ Aggregator.withName

  def valueSource: Parser[ValueSource] = floatingPointNumber ^^ {
    case num => ValueSource(num.toDouble)
  }

  def word: Parser[String] = """[a-zA-Z][_a-zA-Z0-9.-]*""".r

  def metricTag: Parser[(String, String)] = (word <~ ":") ~ word ^^ {
    case name ~ value => name -> value
  }

  def metricTags: Parser[Map[String, String]] = repsep(metricTag, ",") ^^ {
    case list => list.toMap
  }

  def metricSourceWithTags: Parser[MetricSource] = word ~ ("[" ~> metricTags <~ "]") ^^ {
    case metric ~ tagMap => MetricSource(metric, tagMap)
  }

  def metricSourceWithoutTags: Parser[MetricSource] = word ^^ {
    case metric => MetricSource(metric, Map.empty[String, String])
  }

  def metricSource = metricSourceWithTags | metricSourceWithoutTags

  def duration: Parser[FiniteDuration] = wholeNumber ~ ("s" | "m" | "h" | "d") ^^ {
    case time ~ "s" => time.toInt.seconds
    case time ~ "m" => time.toInt.minutes
    case time ~ "h" => time.toInt.hours
    case time ~ "d" => time.toInt.days
  }

  def dailyHours: Parser[LocalTime] = ("@" ~> wholeNumber) ^^ {
    case hours => new LocalTime(hours.toInt, 0)
  }

  def dailyMinutes: Parser[LocalTime] = ("@" ~> wholeNumber) ~ (":" ~> wholeNumber) ^^ {
    case hours ~ minutes => new LocalTime(hours.toInt, minutes.toInt)
  }

  def dailySeconds: Parser[LocalTime] = ("@" ~> wholeNumber) ~ (":" ~> wholeNumber) ~ (":" ~> wholeNumber) ^^ {
    case hours ~ minutes ~ seconds => new LocalTime(hours.toInt, minutes.toInt, seconds.toInt)
  }

  def daily: Parser[LocalTime] = dailySeconds | dailyMinutes | dailyHours

  def anyPeriod = duration | daily

  def repeater: Parser[Int] = "at least" ~> wholeNumber <~ "times" ^^ {
    case num => num.toInt
  }

  def delay: Parser[FiniteDuration] = "delayed by" ~> duration ^^ {
    case duration => duration
  }

  def aggregatedSource: Parser[AggregatedSource] = metricSource ~ ("." ~> aggregator) ~ ("." ~> duration) ^^ {
    case met ~ agg ~ dur => AggregatedSource(met, agg, dur)
  }

  def anySimpleSource: Parser[Source] = valueSource | aggregatedSource | metricSource

  def factoredSourceLeft: Parser[FactoredSource] = (floatingPointNumber <~ "*") ~ anySimpleSource ^^ {
    case factor ~ source => FactoredSource(source, factor.toDouble)
  }

  def factoredSourceRight: Parser[FactoredSource] = anySimpleSource ~ ("*" ~> floatingPointNumber) ^^ {
    case source ~ factor => FactoredSource(source, factor.toDouble)
  }

  def anySource: Parser[Source] = factoredSourceRight | factoredSourceLeft | anySimpleSource

  def missingDataAlert: Parser[MissingDataAlert] = metricSource ~ ("missing for" ~> duration) ^^ {
    case source ~ d => MissingDataAlert(source, d)
  }

  def simpleAlert: Parser[SimpleAlert] = anySource ~ operator ~ anySource ^^ {
    case sourceLeft ~ op ~ sourceRight => SimpleAlert(sourceLeft, op, sourceRight, 1, 0.minutes)
  }

  def simpleAlertWithRepeater: Parser[SimpleAlert] = anySource ~ operator ~ anySource ~ repeater ^^ {
    case sourceLeft ~ op ~ sourceRight ~ num => SimpleAlert(sourceLeft, op, sourceRight, num, 0.minutes)
  }

  def simpleAlertWithDelay: Parser[SimpleAlert] = anySource ~ operator ~ anySource ~ delay ^^ {
    case sourceLeft ~ op ~ sourceRight ~ delay => SimpleAlert(sourceLeft, op, sourceRight, 1, delay)
  }

  def simpleAlertWithRepeaterAndDelay: Parser[SimpleAlert] = anySource ~ operator ~ anySource ~ repeater ~ delay ^^ {
    case sourceLeft ~ op ~ sourceRight ~ num ~ delay => SimpleAlert(sourceLeft, op, sourceRight, num, delay)
  }

  // order is important here: look for the more complex case first
  def anyAlert: Parser[AlertEntity] = missingDataAlert | simpleAlertWithRepeaterAndDelay | simpleAlertWithRepeater | simpleAlertWithDelay | simpleAlert
}
