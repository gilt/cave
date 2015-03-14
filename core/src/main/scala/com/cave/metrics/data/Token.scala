package com.cave.metrics.data

import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.format.ISODateTimeFormat.{dateTime, dateTimeParser}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Token(id: Option[String], description: String, value: String, created: DateTime)

object Token {
  final val KeyId = "id"
  final val KeyDescription = "description"
  final val KeyValue = "value"
  final val KeyCreated = "created"

  final val DefaultName = "default"

  implicit val datetimeReads: Reads[DateTime] =  __.read[String].map(dateTimeParser.parseDateTime)
  implicit val datetimeWrites = new Writes[DateTime] {
    def writes(value: DateTime) = JsString(dateTime.print(value))
  }


  implicit val tokenReads: Reads[Token] = (
      (__ \ KeyId).readNullable[String] and
      (__ \ KeyDescription).read[String] and
      (__ \ KeyValue).read[String] and
      (__ \ KeyCreated).read[DateTime]
    )(Token.apply _)

  implicit val tokenWrites: Writes[Token] = (
      (__ \ KeyId).writeNullable[String] and
      (__ \ KeyDescription).write[String] and
      (__ \ KeyValue).write[String] and
      (__ \ KeyCreated).write[DateTime]
    )(unlift(Token.unapply))

  val secureRandom = new java.security.SecureRandom

  def createToken(description: String): Token =
    new Token(None, description,
      RandomStringUtils.random(56, 0, 0, true, true, null, secureRandom),
      new DateTime().withZone(DateTimeZone.UTC)
    )
}