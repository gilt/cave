package com.cave.metrics.data

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class User(id: Option[Long], firstName: String, lastName: String, email: String, password: String, salt: Option[String])
case class UserPatch(firstName: Option[String], lastName: Option[String], password: Option[String])
case class UserConfirmation(firstName: String, lastName: String, password: String, confirmationToken: String)
case class PasswordInfo(hash: String, salt: Option[String])

object User {
  implicit val userWrites = new Writes[User] {
    override def writes(obj: User): JsValue = Json.obj(
      "first_name" -> obj.firstName,
      "last_name" -> obj.lastName,
      "email" -> obj.email
    )
  }
}

object UserPatch {
  implicit val userPatchReads: Reads[UserPatch] = (
      (__ \ "first_name").readNullable[String] and
      (__ \ "last_name").readNullable[String] and
      (__ \ "password").readNullable[String]
    )(UserPatch.apply _)
}

object UserConfirmation {
  implicit val userConfirmationReads: Reads[UserConfirmation] = (
      (__ \ "first_name").read[String] and
      (__ \ "last_name").read[String] and
      (__ \ "password").read[String] and
      (__ \ "confirmation_token").read[String]
    )(UserConfirmation.apply _)

  implicit val userConfirmationWrites: Writes[UserConfirmation] = (
    (__ \ "first_name").write[String] and
      (__ \ "last_name").write[String] and
      (__ \ "password").write[String] and
      (__ \ "confirmation_token").write[String]
    )(unlift(UserConfirmation.unapply))
}

case class Role(value: String) {
  def is(role: Role): Boolean = this == role
  def isValid: Boolean = Role.Everyone.exists(_.value == value)
}

object Role {
  // Admin has full rights
  val Admin  = Role("admin")

  // A user who cannot invite others
  val Member = Role("member")

  // A read-only user
  val Viewer = Role("viewer")

  // Special role for organization user attached only to a team
  val Team = Role("team")

  // Lists of roles
  val AdminsOnly = List(Admin)
  val AtLeastMember = List(Admin, Member)
  val Everyone = List(Admin, Member, Viewer)

  implicit val roleReads: Reads[Role] = __.read[String] map Role.apply
  implicit val roleWrites = new Writes[Role] {
    override def writes(o: Role): JsValue = JsString(o.value)
  }

  implicit val listRolesWrites = new Writes[List[(String, Role)]] {
    override def writes(list: List[(String, Role)]): JsValue = Json.toJson(list map { case (name, role) =>
      Json.obj("name" -> name, "role" -> Json.toJson(role))
    })
  }

  implicit val listUsersWrites = new Writes[List[(User, Role)]] {
    override def writes(list: List[(User, Role)]): JsValue = Json.toJson(list map { case (user, role) =>
      Json.obj("user" -> Json.toJson(user), "role" -> Json.toJson(role))
    })
  }
}
