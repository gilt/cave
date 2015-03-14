package com.cave.metrics.data.postgresql

import java.util.UUID

import com.cave.metrics.data.{Alert, Organization}
import org.scalatest.BeforeAndAfter

import scala.util.Success

class CacheDataManagerSpec extends AbstractDataManagerSpec with BeforeAndAfter {

  var dm: PostgresCacheDataManagerImpl = _

  before {
    dm = new PostgresCacheDataManagerImpl(awsConfig)
  }

  "Cache Data Manager" should "return organizations found in DB, even without alerts" in {
    dm.getEnabledAlerts() match {
      case Success(map) =>
        map.size should be (2)
        map(GiltOrgName).size should be (0)
        map(SecondOrgName).size should be (0)

      case _ => fail("unable to find alerts ")
    }
  }

  it should "be able to retrieve all active alerts" in {
    val giltOrg = Organization(Some(GiltOrganizationId.toString), GiltOrgName, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))
    val alert = Alert(None, "my_alert=" + UUID.randomUUID().toString, enabled = true, "15m", "condition=true", None, None, None)
    for (i <- 1 to 30) dm.createOrganizationAlert(giltOrg, alert, Set())

    // remove 5 alerts
    5 until 10 map (id => dm.deleteAlert(id.toString))

    dm.getEnabledAlerts() match {
      case Success(scheduleMap) =>
        scheduleMap.size should be(2)
        scheduleMap(SecondOrgName).size should be (0)
        scheduleMap(GiltOrgName).size should be(25)

      case _ => fail("unable to find alerts ")
    }
  }
}
