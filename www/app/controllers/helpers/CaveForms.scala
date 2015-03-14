package controllers.helpers

import org.apache.commons.codec.net.URLCodec
import play.api.data.Form
import play.api.data.Forms._

object CaveForms {

  val createTokenForm = Form(
    mapping(
      "organization" -> nonEmptyText,
      "team" -> optional(text),
      "description" -> text
    )(CreateToken.apply)(CreateToken.unapply)
  )

  val deleteTokenForm = Form(
    mapping(
      "organization" -> nonEmptyText,
      "team" -> optional(text),
      "tokenId" -> nonEmptyText
    )(DeleteToken.apply)(DeleteToken.unapply)
  )

  val editAlertForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "organization" -> nonEmptyText,
      "team" -> optional(text),
      "status" -> boolean,
      "description" -> text,
      "period" -> nonEmptyText,
      "handbookUrl" -> text,
      "routing" -> optional(text),
      "condition" -> optional(text)
    )(EditAlert.apply)(EditAlert.unapply)
  )
  val createAlertForm = Form(
    mapping(
      "organization" -> nonEmptyText,
      "team" -> text,
      "status" -> boolean,
      "description" -> text,
      "period" -> nonEmptyText,
      "condition" -> text,
      "handbookUrl" -> text,
      "routing" -> optional(text)
    )(CreateAlert.apply)(CreateAlert.unapply)
  )

  val deleteAlertForm = Form(
    mapping(
      "organization" -> nonEmptyText,
      "team" -> optional(text),
      "alertId" -> nonEmptyText
    )(DeleteAlert.apply)(DeleteAlert.unapply)
  )

  val addUserForm = Form(
    mapping("email" -> email,
      "organization" -> nonEmptyText,
      "team" -> text,
      "role" -> nonEmptyText
    )(AddUserData.apply)(AddUserData.unapply)
  )

  val createTeamForm = Form(
    mapping("name" -> nonEmptyText.verifying("Illegal characters", v => new URLCodec().encode(v).equals(v)),
      "organization" -> nonEmptyText
    )(CreateTeam.apply)(CreateTeam.unapply)
  )

  val removeUserForm = Form(
    mapping("email" -> email,
      "organization" -> nonEmptyText,
      "team" -> text
    )(UserDelete.apply)(UserDelete.unapply)
  )

  val organizationForm = Form(
    mapping(
      "name" -> nonEmptyText.verifying("Illegal characters", v => new URLCodec().encode(v).equals(v)),
      "email" -> email,
      "notificationUrl" -> text
    )(NewOrganization.apply)(NewOrganization.unapply)
  )
}

case class AddUserData(email: String, orgName: String, teamName: String, role: String)

case class CreateToken(orgName: String, teamName: Option[String], description: String)

case class DeleteToken(orgName: String, teamName: Option[String], tokenId: String)

case class UserDelete(email: String, orgName: String, teamName: String)

case class CreateTeam(teamName: String, orgName: String)

case class NewOrganization(name: String, email: String, notificationUrl: String)

case class CreateAlert(organization: String, team: String, status: Boolean, description: String, period: String, condition: String, handbookUrl: String, routing: Option[String])

case class EditAlert(alertId: String, organization: String, team: Option[String], status: Boolean, description: String, period: String, handbookUrl: String, routing: Option[String], condition: Option[String])

case class DeleteAlert(organization: String, team: Option[String], alertId: String)