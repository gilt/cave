package controllers

import org.joda.time.{DateTime, Period}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class GraphDataSpec extends FlatSpec with Matchers with BeforeAndAfter {

  "Metric Time Range" should "find the first data point timestamp" in {
    val graphs = new Graphs
    val now = new DateTime()

    val period = Period.minutes(3)
    val firstResultTimestampFromDb = now.minusDays(1)
    val startTimeRange = now.minusDays(2)

    val start = graphs.findStartDate(firstResultTimestampFromDb, startTimeRange, period)

    println(s"FIRST RESULT TIME=      $firstResultTimestampFromDb")
    println(s"START TIME RANGE=       $startTimeRange")
    println(s"CALCULATED START TIME=  $start")

    assert(firstResultTimestampFromDb.toDate.getTime >= startTimeRange.toDate.getTime, "FIRST RESULT Start time is NOT after expected Start time range")
    assert(firstResultTimestampFromDb.toDate.getTime >= start.toDate.getTime, "FIRST RESULT Start time is NOT after expected CALCULATED Start time")

    assert(start.toDate.getTime >= startTimeRange.toDate.getTime, "Start time is NOT after expected CALCULATED Start time")
    assert(start.minus(period).toDate.getTime <= startTimeRange.toDate.getTime, "Start time MINUS PERIOD is NOT before expected Start time")

  }

  it should "find the first data point timestamp when the first DB result timestamp is the same as the beginning of the time range" in {
    val graphs = new Graphs
    val now = new DateTime()

    val period = Period.minutes(3)
    val firstResultTimestampFromDb = now.minusDays(1)
    val startTimeRange = firstResultTimestampFromDb

    val start = graphs.findStartDate(firstResultTimestampFromDb, startTimeRange, period)

    println(s"FIRST RESULT TIME=      $firstResultTimestampFromDb")
    println(s"START TIME RANGE=       $startTimeRange")
    println(s"CALCULATED START TIME=  $start")

    assert(firstResultTimestampFromDb.toDate.getTime >= startTimeRange.toDate.getTime, "FIRST RESULT Start time is NOT after expected Start time range")
    assert(firstResultTimestampFromDb.toDate.getTime >= start.toDate.getTime, "FIRST RESULT Start time is NOT after expected CALCULATED Start time")

    assert(start.toDate.getTime >= startTimeRange.toDate.getTime, "Start time is NOT after expected CALCULATED Start time")
    assert(start.minus(period).toDate.getTime <= startTimeRange.toDate.getTime, "Start time MINUS PERIOD is NOT before expected Start time")
    assert(start.isEqual(startTimeRange))
  }

  it should "find the first expected result time when the first result date is the same as the start" in {
    val graphs = new Graphs
    val now = new DateTime()

    val period = Period.minutes(3)
    val firstResultTimestampFromDb = now.minusDays(1)
    val startTimeRange = firstResultTimestampFromDb.plus(period)

    val start = graphs.findStartDate(firstResultTimestampFromDb, startTimeRange, period)

    println(s"FIRST RESULT TIME=      $firstResultTimestampFromDb")
    println(s"START TIME RANGE=       $startTimeRange")
    println(s"CALCULATED START TIME=  $start")

    assert(firstResultTimestampFromDb.toDate.getTime >= start.toDate.getTime, "FIRST RESULT Start time is NOT after expected CALCULATED Start time")
    assert(start.minus(period).toDate.getTime <= startTimeRange.toDate.getTime, "Start time MINUS PERIOD is NOT before expected Start time")
    assert(start.isEqual(firstResultTimestampFromDb))
  }

}

