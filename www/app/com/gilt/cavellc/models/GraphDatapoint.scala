package com.gilt.cavellc.models

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.ISODateTimeFormat._
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class GraphDatapoint(timestamp: DateTime, value: Option[Double])

object GraphDatapoint {
  implicit val datetimeWrites = new Writes[DateTime] {
    def writes(value: DateTime) = JsString(dateTime.print(value))
  }

  implicit val graphDataWrites: Writes[GraphDatapoint] = (
    (__ \ "ts").write[DateTime] and
      (__ \ "value").write[Option[Double]]
    )(unlift(GraphDatapoint.unapply))

  def parseDateTime(dateString: Option[String]): Option[DateTime] =
    dateString map ISODateTimeFormat.timeNoMillis().parseDateTime
}