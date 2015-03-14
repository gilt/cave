package com.cave.metrics.data

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import org.joda.time.format.ISODateTimeFormat._

case class CaveIssue(description: String, since: DateTime, until: Option[DateTime])
case class CaveStatus(current: Seq[CaveIssue], recent: Seq[CaveIssue])


object CaveIssue {

  final val KeyDescription = "description"
  final val KeySince = "since"
  final val KeyUntil = "until"

  private implicit val jsonReadsJodaDateTime = __.read[String].map(dateTimeParser.parseDateTime(_))

  private implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
    def writes(x: org.joda.time.DateTime) = JsString(dateTime.print(x))
  }

  implicit val issueReads: Reads[CaveIssue] = (
    (__ \ KeyDescription).read[String] and
      (__ \ KeySince).read[DateTime] and
      (__ \ KeyUntil).readNullable[DateTime]
    )(CaveIssue.apply _)

  implicit val issueWrites: Writes[CaveIssue] = (
    (__ \ KeyDescription).write[String] and
      (__ \ KeySince).write[DateTime] and
      (__ \ KeyUntil).writeNullable[DateTime]
    )(unlift(CaveIssue.unapply))
}

object CaveStatus {
  final val KeyCurrent = "current"
  final val KeyRecent  = "recent"

  implicit val statusReads: Reads[CaveStatus] = (
    (__ \ KeyCurrent).read[Seq[CaveIssue]] and
      (__ \ KeyRecent).read[Seq[CaveIssue]]
    )(CaveStatus.apply _)

  implicit val statusWrites: Writes[CaveStatus] = (
    (__ \ KeyCurrent).write[Seq[CaveIssue]] and
      (__ \ KeyRecent).write[Seq[CaveIssue]]
    )(unlift(CaveStatus.unapply))
}