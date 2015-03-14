package com.cave.metrics.data.postgresql

import java.sql.Timestamp

import com.cave.metrics.data.postgresql.Tables._
import org.joda.time.format.DateTimeFormat
import org.scalatest.BeforeAndAfter
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.StaticQuery

class SchedulerDataManagerSpec extends AbstractDataManagerSpec with BeforeAndAfter {
  val hostname_1 = "host1"
  val hostname_2 = "host2"
  val hostname_3 = "host3"

  var dm: SchedulerDataManager = _

  before {
    dm = new SchedulerDataManager(awsConfig) {
      override def DBDateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss")

      override def leadershipTermTimeoutSeconds = 30
    }
    Schedulers += SchedulersRow(1, "initialValue", new Timestamp(System.currentTimeMillis() - 1000 * 60))
  }

  "Scheduler Data Manager" should "update Schedulers table" in {
    Schedulers.list.head.name should be("initialValue")

    assert(dm.takeLeadership(hostname_1), "Expected success")
    Schedulers.list.head.name should be(hostname_1)

    assert(dm.takeLeadership(hostname_3) == false, "Expected success")
    Schedulers.list.head.name should be(hostname_1)

    assert(dm.extendLeadership(hostname_2) == false, "Expected success")
    Schedulers.list.head.name should be(hostname_1)

    Thread.sleep(1500)
    assert(dm.extendLeadership(hostname_1), "A-hostname was not able to extend its leadership")
    Schedulers.list.head.name should be(hostname_1)
  }

  it should "not update the leader if one is active" in {
    StaticQuery.queryNA("truncate table SCHEDULERS").execute
    Schedulers += SchedulersRow(1, hostname_1, new Timestamp(System.currentTimeMillis() - 1000 * 20))
    Schedulers.list.length should be(1)
    assert(!dm.takeLeadership(hostname_2), "Expected failure")
    Schedulers.list.head.name should be(hostname_1)

    Thread.sleep(100)
    assert(dm.extendLeadership(hostname_1), "Expected success")
    Schedulers.list.head.name should be(hostname_1)

  }

  it should "not give leadership to host3 when host2 is the leader" in {
    StaticQuery.queryNA("truncate table SCHEDULERS").execute
    Schedulers += SchedulersRow(1, hostname_1, new Timestamp(System.currentTimeMillis() - 1000 * 31))
    Schedulers.list.length should be(1)
    assert(dm.takeLeadership(hostname_2), "Expected success")
    Schedulers.list.head.name should be(hostname_2)


    assert(!dm.takeLeadership(hostname_3), "Expected failure")
    Schedulers.list.head.name should be(hostname_2)

    assert(!dm.takeLeadership(hostname_1), "Expected failure")
    Schedulers.list.head.name should be(hostname_2)
  }

  it should "be thread safe" in {
    StaticQuery.queryNA("truncate table SCHEDULERS").execute
    Schedulers.list.length should be(0)
    Schedulers += SchedulersRow(1, hostname_1, new Timestamp(System.currentTimeMillis() - 1000 * 360))
    Schedulers.list.length should be(1)
    Schedulers.list.head.name should be(hostname_1)

    import scala.slick.jdbc.{GetResult, StaticQuery => Q}

    /* Blocking Schedulers table */
    val sql = s"BEGIN; select * from SCHEDULERS FOR UPDATE"
    val query = Q.queryNA[SchedulersRow](sql)
    query.list.length should be(1)


    assert(!dm.takeLeadership(hostname_1), "Expected failure")
    assert(!dm.takeLeadership(hostname_2), "Expected failure")
    assert(!dm.takeLeadership(hostname_3), "Expected failure")
    assert(!dm.extendLeadership(hostname_1), "Expected failure")
    assert(!dm.extendLeadership(hostname_2), "Expected failure")
    assert(!dm.extendLeadership(hostname_3), "Expected failure")

    Schedulers.list.head.name should be(hostname_1)
  }
}
