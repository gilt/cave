package com.cave.metrics.data

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Organization(id: Option[String], name: String, email: String, notificationUrl: String, tokens: Option[Seq[Token]], influxCluster: Option[String] = None)
case class OrganizationPatch(email: Option[String], notificationUrl: Option[String])

object Organization {
  final val KeyId = "id"
  final val KeyName = "name"
  final val KeyEmail = "email"
  final val KeyNotificationUrl = "notification_url"
  final val KeyTokens = "tokens"
  final val KeyCluster = "cluster"

  implicit val orgReads: Reads[Organization] = (
    (__ \ KeyId).readNullable[String] and
      (__ \ KeyName).read[String] and
      (__ \ KeyEmail).read[String] and
      (__ \ KeyNotificationUrl).read[String] and
      (__ \ KeyTokens).readNullable[Seq[Token]] and
      (__ \ KeyCluster).readNullable[String]
    )(Organization.apply _)

  implicit val orgWrites = new Writes[Organization] {
    override def writes(obj: Organization): JsValue = Json.obj(
      KeyName -> obj.name,
      KeyEmail -> obj.email,
      KeyNotificationUrl -> obj.notificationUrl,
      KeyTokens -> Json.toJson(obj.tokens)
    )
  }
}

object OrganizationPatch {
  final val KeyEmail = "email"
  final val KeyNotificationUrl = "notification_url"

  implicit val orgPatchReads: Reads[OrganizationPatch] = (
    (__ \ KeyEmail).readNullable[String] and
    (__ \ KeyNotificationUrl).readNullable[String]
  )(OrganizationPatch.apply _)

  implicit val orgPatchWrites: Writes[OrganizationPatch] = (
    (__ \ KeyEmail).writeNullable[String] and
    (__ \ KeyNotificationUrl).writeNullable[String]
  )(unlift(OrganizationPatch.unapply))
}