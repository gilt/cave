package com.cave.metrics.data

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

case class Check(schedule: Schedule, timestamp: DateTime)

object Check {

  final val FMT = ISODateTimeFormat.dateTimeNoMillis()

  implicit val checkReads = new Reads[Check] {
    def reads(value: JsValue) = try {
      JsSuccess(new Check(
        (value \ "schedule").as[Schedule],
        FMT.parseDateTime((value \ "timestamp").as[String])
      ))
    } catch {
      case e: Exception => JsError(e.getMessage)
    }
  }

  implicit val checkWrites = new Writes[Check] {
    def writes(check: Check): JsValue = {
      Json.obj(
        "schedule" -> Json.toJson(check.schedule),
        "timestamp" -> JsString(FMT.print(check.timestamp))
      )
    }
  }
}
