package com.cave.metrics.data

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Schedule(orgName: String, teamName: Option[String], clusterName: Option[String], notificationUrl: String, alert: Alert) {
  def databaseName = teamName.map(_ + ".").getOrElse("") + orgName
}

object Schedule {
  final val KeyOrganization = "organization"
  final val KeyTeam = "team"
  final val KeyCluster = "cluster"
  final val KeyNotificationUrl = "notification_url"
  final val KeyAlert = "alert"

  implicit val scheduleReads: Reads[Schedule] = (
      (__ \ KeyOrganization).read[String] and
      (__ \ KeyTeam).readNullable[String] and
      (__ \ KeyCluster).readNullable[String] and
      (__ \ KeyNotificationUrl).read[String] and
      (__ \ KeyAlert).read[Alert]
    )(Schedule.apply _)

  implicit val scheduleWrites: Writes[Schedule] = (
      (__ \ KeyOrganization).write[String] and
      (__ \ KeyTeam).writeNullable[String] and
      (__ \ KeyCluster).writeNullable[String] and
      (__ \ KeyNotificationUrl).write[String] and
      (__ \ KeyAlert).write[Alert]
    )(unlift(Schedule.unapply))
}
