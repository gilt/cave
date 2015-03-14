package com.cave.metrics.data

import org.scalatest.{Matchers, FlatSpec}
import play.api.libs.json.Json

class CheckSpec extends FlatSpec with Matchers with AlertJsonData {

  "A check" should "be parsed from json" in {
    val check = CheckJson.as[Check]

    check.schedule should have(
      'orgName(OrgName),
      'teamName(Some(TeamName)),
      'notificationUrl(NotificationUrl)
    )
    check.schedule.alert should have(
      'id(None),
      'description(AlertDescription),
      'enabled(AlertEnabled),
      'period(AlertPeriod),
      'condition(AlertCondition)
    )
    check.timestamp should be(Timestamp)
  }

  it should "be converted into json" in {
    val check = CheckJson.as[Check]

    val json = Json.stringify(Json.toJson(check))
    val check2 = Json.parse(json).as[Check]

    check.schedule.orgName should be(check2.schedule.orgName)
    check.schedule.teamName should be(check2.schedule.teamName)
    check.schedule.notificationUrl should be(check2.schedule.notificationUrl)
    check.schedule.alert.description should be (check2.schedule.alert.description)
    check.schedule.alert.enabled should be (check2.schedule.alert.enabled)
    check.schedule.alert.period should be (check2.schedule.alert.period)
    check.schedule.alert.condition should be (check2.schedule.alert.condition)
    check.timestamp should be (check2.timestamp)
  }
}
