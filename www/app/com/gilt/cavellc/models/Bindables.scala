package com.gilt.cavellc.models

object Bindables {

  import org.joda.time.format.ISODateTimeFormat
  import org.joda.time.{DateTime, LocalDate}
  import play.api.mvc.{PathBindable, QueryStringBindable}

  // Type: date-time-iso8601
  implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[DateTime](
    ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
  )

  // Type: date-time-iso8601
  implicit val queryStringBindableTypeDateTimeIso8601 = new QueryStringBindable.Parsing[DateTime](
    ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
  )

  // Type: date-iso8601
  implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[LocalDate](
    ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29"
  )

  // Enum: Role
  private val enumRoleNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${Role.all.mkString(", ")}"

  private val enumAggregatorNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${Aggregator.all.mkString(", ")}"

  implicit val pathBindableEnumRole = new PathBindable.Parsing[Role](
    Role.fromString(_).get, _.toString, enumRoleNotFound
  )

  implicit val queryStringBindableEnumRole = new QueryStringBindable.Parsing[Role](
    Role.fromString(_).get, _.toString, enumRoleNotFound
  )

  implicit val queryStringBindableAggregator = new QueryStringBindable.Parsing[Aggregator](
    Aggregator.fromString(_).get, _.toString, enumAggregatorNotFound
  )

}