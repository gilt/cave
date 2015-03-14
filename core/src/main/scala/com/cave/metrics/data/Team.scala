package com.cave.metrics.data

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Team(id: Option[String], name: String, tokens: Option[Seq[Token]], influxCluster: Option[String] = None)

object Team {
  final val KeyId = "id"
  final val KeyName = "name"
  final val KeyTokens = "tokens"
  final val KeyCluster = "cluster"

  implicit val teamReads: Reads[Team] = (
    (__ \ KeyId).readNullable[String] and
    (__ \ KeyName).read[String] and
    (__ \ KeyTokens).readNullable[Seq[Token]] and
    (__ \ KeyCluster).readNullable[String]
  )(Team.apply _)

  implicit val teamWrites = new Writes[Team] {
    override def writes(obj: Team): JsValue = Json.obj(
      KeyName -> obj.name,
      KeyTokens -> Json.toJson(obj.tokens)
    )
  }
}