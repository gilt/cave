package com.cave.metrics.data

import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json

trait AlertJsonData {

  final val ResponseTimeSvc17    = "response-time [service: svc-sale-selector, env: production, host: svc17.prod.iad]"
  final val ResponseTime5Mp99    = "response-time [service: svc-important, env: production].p99.5m"
  final val ResponseTimeDailyAvg = "response-time [service: svc-important, env: production].mean.1d"

  final val AlertDescription = "p99 of ResponseTime is above the daily average for more than 5 minutes"
  final val AlertEnabled = true
  final val AlertDisabled = false
  final val AlertPeriod = "5m"
  final val AlertCondition = s"$ResponseTime5Mp99 > $ResponseTimeDailyAvg"
  final val AlertHandbookUrl = "https://over.here/alert"
  final val AlertRouting = Map("key" -> "something extra")

  final val OtherAlertDescription = "p99 of Errors is above the daily average for more than 7 minutes"
  final val OtherAlertEnabled = true
  final val OtherAlertPeriod = "7m"
  final val OtherAlertCondition = s"$ResponseTimeSvc17 < $ResponseTime5Mp99"
  final val OtherAlertHandbookUrl = "https://over.there/alert"
  final val OtherAlertRouting = Map("key" -> "something else extra")

  final val AlertJson = Json.obj(
    "description" -> AlertDescription,
    "enabled" -> AlertEnabled,
    "period" -> AlertPeriod,
    "condition" -> AlertCondition,
    "handbook" -> AlertHandbookUrl,
    "routing" -> AlertRouting
  )

  final val AlertBadJson = Json.obj(
    "description" -> "This alert has no period",
    "enabled" -> AlertEnabled,
    "condition" -> AlertCondition,
    "handbook" -> AlertHandbookUrl,
    "routing" -> AlertRouting
  )

  final val OrgName = "gilt"
  final val TeamName = "twain"
  final val DatabaseName = "twain.gilt"
  final val NotificationUrl = "http://cave.gilt.com/incoming"

  final val ScheduleJson = Json.obj(
    "organization" -> OrgName,
    "team" -> TeamName,
    "notification_url" -> NotificationUrl,
    "alert" -> AlertJson
  )

  final val Format = ISODateTimeFormat.dateTimeNoMillis()
  final val TimestampJson = "2014-07-22T10:09:30Z"
  final val Timestamp = Format.parseDateTime(TimestampJson)

  final val CheckJson = Json.obj(
     "schedule" -> ScheduleJson,
     "timestamp" -> TimestampJson
  )

  final val Timestamp0 = Format.parseDateTime("2014-08-06T11:00:00Z")
  final val Timestamp1 = Format.parseDateTime("2014-08-06T11:01:00Z")
  final val Timestamp2 = Format.parseDateTime("2014-08-06T11:02:00Z")
  final val Timestamp3 = Format.parseDateTime("2014-08-06T11:03:00Z")
  final val Timestamp4 = Format.parseDateTime("2014-08-06T11:04:00Z")


  final val InsufficientOrders = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl,
    Alert(None, AlertDescription, AlertEnabled, AlertPeriod, "orders[ship: US] < 5", None, None)), Timestamp)

  final val AlertFiveDescription = "Five"
  final val InsufficientOrdersFive = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl,
    Alert(None, AlertFiveDescription , AlertEnabled, AlertPeriod, "orders[ship: US] <= 10 at least 5 times", None, None)), Timestamp)

  final val AlertPredictedDescription = "Predicted"
  final val AlternativeClusterName = Some("magic")
  final val OrdersLessThanPredicted = Check(Schedule(OrgName, Some(TeamName), AlternativeClusterName, NotificationUrl,
    Alert(None, AlertPredictedDescription, AlertEnabled, AlertPeriod, "orders[ship: US] <= ordersLO[ship: US] at least 5 times", None, None)), Timestamp)
}
