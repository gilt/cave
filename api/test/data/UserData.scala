package data

import java.util.UUID

import com.cave.metrics.data._
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json

trait UserData {

  val SOME_FIRST = "Joe"
  val SOME_LAST = "Appleseed"
  val SOME_EMAIL = "joe.appleseed@gmail.com"
  val SOME_PASSWORD = "123456"
  val SOME_HASH = "$2a$10$RNJFp9/qPuyMSwgjZanWnOuCBSTCISiBMQ7Im/3PNn2aLhFTYMU6q"
  val SOME_SALT = Some("$2a$10$RNJFp9/qPuyMSwgjZanWnO")
  val SOME_INFO = PasswordInfo(SOME_HASH, SOME_SALT)

  val SOME_UUID = UUID.randomUUID.toString
  val SOME_DATETIME = ISODateTimeFormat.dateTimeNoMillis.parseDateTime("2014-10-10T12:10:00Z")
  val SOME_DATETIME_PLUS_1H = ISODateTimeFormat.dateTimeNoMillis.parseDateTime("2014-10-10T13:10:00Z")
  val LATE_DATETIME = ISODateTimeFormat.dateTimeNoMillis.parseDateTime("2014-10-11T12:10:00Z")

  val EXPIRATION1 = LATE_DATETIME.minusSeconds(1)
  val EXPIRATION2 = LATE_DATETIME.plusSeconds(1)

  val SOME_CONFIRMATION_TOKEN = ConfirmationToken(Some(1L), SOME_UUID, SOME_EMAIL, SOME_DATETIME, LATE_DATETIME, isSignUp = true)

  val USER_ID = 1L
  val SOME_USER_EMAIL = "jappleseed@gilt.com"
  val SOME_USER = User(Some(USER_ID), SOME_FIRST, SOME_LAST, SOME_USER_EMAIL, SOME_HASH, SOME_SALT)

  val SOME_ID = 1234L
  val SOME_TOKEN = "4D865D33-2D3B-48D0-A411-4DA176C2D1F8"
  val SOME_SESSION_TOKEN = SessionToken(Some(SOME_ID), USER_ID, SOME_TOKEN, SOME_DATETIME, LATE_DATETIME)

  val AUTH_TOKEN = "Bearer " + SOME_TOKEN
  val BASIC_AUTH_TOKEN = "Basic " + new sun.misc.BASE64Encoder().encodeBuffer((SOME_TOKEN + ":").getBytes("UTF-8"))
  val UNSUPPORTED_TOKEN = "Digest " + SOME_TOKEN

  val NEW_FIRST = "Mark"
  val NEW_LAST = "Bumblebee"
  val NEW_PASSWORD = "654321"
  val NEW_HASH = "$2a$10$RNJFp9/qPuyMSwgjZanWnOuCBSTCISiBMQ7Im/3PNn2aLhFTYMU6q"
  val NEW_SALT = Some("$2a$10$RNJFp9/qPuyMSwgjZanWnO")
  val NEW_INFO = PasswordInfo(NEW_HASH, NEW_SALT)

  def getPatchedUser(user: User, newFirst: Option[String], newLast: Option[String], newInfo: Option[PasswordInfo]) =
    User(user.id,
      newFirst getOrElse user.firstName,
      newLast getOrElse user.lastName,
      user.email,
      newInfo.map(_.hash) getOrElse user.password,
      newInfo.map(_.salt) getOrElse user.salt
    )

  val SOME_ORG_NAME1 = "org1"
  val SOME_ORG_NAME2 = "org2"
  val SOME_ORG_LIST = List(SOME_ORG_NAME1 -> Role.Admin, SOME_ORG_NAME2 -> Role.Member)

  val SOME_TEAM_NAME1 = "team1"
  val SOME_TEAM_NAME2 = "team2"
  val SOME_TEAM_LIST = List(SOME_TEAM_NAME1 -> Role.Member, SOME_TEAM_NAME2 -> Role.Admin)


  val USER1 = User(Some(1L), "One", "One", "one@one.com", "one", None)
  val USER2 = User(Some(2L), "Two", "Two", "two@two.com", "two", None)

  val SOME_USER_LIST = List(USER1, USER2)

  val SOME_USERS = List(USER1 -> Role.Admin, USER2 -> Role.Member)
  val SOME_USERS_JSON = Json.arr(
    Json.obj(
      "user" -> Json.toJson(USER1),
      "role" -> Json.toJson(Role.Admin)
    ),
    Json.obj(
      "user" -> Json.toJson(USER2),
      "role" -> Json.toJson(Role.Member)
    )
  )
}
