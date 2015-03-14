package controllers

import com.gilt.cavellc.models._
import com.gilt.cavellc.Client
import com.gilt.cavellc.errors.FailedRequest
import controllers.helpers.CaveForms._
import controllers.helpers.{AddUserData, UserDelete}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UserTeamOrganization(organizationName: String, userTeam: UserTeam)

case class SideMenuData(userOrgs: Seq[UserOrganization], userTeamsInOrgs: Seq[UserTeamOrganization],
                        selectedOrganization: Option[Organization], selectedTeam: Option[Team], roleInOrganization: Option[Role])

class Application extends AbstractCaveController {

  def index = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      for {
        organizations <- client.Users.getOrganizations()
        userTeamOrganization <- createAllUserTeamOrgs(organizations, client)
      } yield Ok(views.html.dashboard(SideMenuData(organizations.sortBy(_.name), userTeamOrganization, None, None, None))())
    }
  }

  def removeUser() = caveAsyncAction { implicit request =>
    removeUserForm.bindFromRequest().fold(handleFormErrors, deleteUser)
  }

  def addUser() = caveAsyncAction { implicit request =>
    addUserForm.bindFromRequest().fold(handleFormErrors, handleAddUser)
  }

  def changeRole() = caveAsyncAction { implicit request =>
    addUserForm.bindFromRequest().fold(handleFormErrors, handleChangeRole)
  }

  private def handleAddUser(addUserData: AddUserData)
                           (implicit request: Request[AnyContent]) = {
    addUserData match {
      case AddUserData(_, _, teamName, _) if teamName.nonEmpty => addUserToTeam(addUserData)
      case _ => addUserToOrganization(addUserData)
    }
  }

  private def addUserToTeam(ud: AddUserData)
                           (implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Teams.postOrganizationsAndUsersByOrganizationAndTeam(ud.orgName, ud.teamName, ud.email, Role(ud.role)).map {
      response =>
        debug(s"Added ${ud.email} to ${ud.teamName} @ ${ud.orgName}")
        Redirect(routes.Teams.team(ud.orgName, ud.teamName))
          .flashing("success" -> Messages("cave.team.addUser.success", ud.email, ud.teamName, ud.orgName))
    } recover {
      case error: FailedRequest if error.responseCode == BAD_REQUEST =>
        Logger.debug("Unable to add a user to team. Bad Request", error)
        Redirect(routes.Teams.team(ud.orgName, ud.teamName)).flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
    }
  }

  private def addUserToOrganization(addUserData: AddUserData)
                                   (implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Organizations.postUsersByName(addUserData.orgName, addUserData.email, Role(addUserData.role)).map {
      response =>
        debug(s"Added ${addUserData.email} to ${addUserData.orgName}")
        Redirect(routes.Organizations.organization(addUserData.orgName))
          .flashing("success" -> Messages("cave.organizations.addUser.success", addUserData.email, addUserData.orgName))
    } recover {
      case error: FailedRequest if error.responseCode == BAD_REQUEST =>
        Logger.debug("Unable to add a user to organization. Bad Request", error)
        Redirect(routes.Organizations.organization(addUserData.orgName)).flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
    }
  }

  private def handleChangeRole(formData: AddUserData)
                              (implicit request: Request[AnyContent]) = formData match {
    case AddUserData(_, _, teamName, _) if teamName.nonEmpty => changeTeamRole(formData)
    case _ => changeOrganizationRole(formData)
  }

  private def changeTeamRole(formData: AddUserData)
                            (implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Teams.patchOrganizationsAndUsersByOrganizationAndTeamAndEmail(formData.orgName, formData.teamName, formData.email, Role(formData.role)) map {
      response =>
        debug(s"Changed user ${formData.email} role to ${formData.role} for team ${formData.teamName} in organization ${formData.orgName}")
        Redirect(routes.Teams.team(formData.orgName, formData.teamName))
          .flashing("success" -> Messages("cave.team.changeRole.success", formData.email, formData.teamName, formData.orgName, formData.role))
    } recover {
      case error: FailedRequest if error.responseCode == BAD_REQUEST =>
        Logger.debug("Failed to change role of user in team, BadRequest", error)
        Redirect(routes.Teams.team(formData.orgName, formData.teamName))
          .flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
    }
  }

  private def changeOrganizationRole(formData: AddUserData)
                                    (implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Organizations.patchUsersByNameAndEmail(formData.orgName, formData.email, Role(formData.role)) map {
      response =>
        debug(s"Changed user ${formData.email} role to ${formData.role} for organization ${formData.orgName}")
        Redirect(routes.Organizations.organization(formData.orgName))
          .flashing("success" -> Messages("cave.organizations.changeRole.success", formData.email, formData.orgName, formData.role))
    } recover {
      case error: FailedRequest if error.responseCode == BAD_REQUEST =>
        Logger.debug("Failed to change role of user in organization, BadRequest", error)
        Redirect(routes.Organizations.organization(formData.orgName))
          .flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
    }
  }

  private def createAllUserTeamOrgs(userOrgs: Seq[UserOrganization], client: Client): Future[Seq[UserTeamOrganization]] = {
    val futureReq = for {
      userOrg <- userOrgs
    } yield createUserTeamOrg(userOrg, client)

    Future.sequence(futureReq).map(_.flatten.sortBy(_.userTeam.name))
  }

  private def createUserTeamOrg(userOrg: UserOrganization, client: Client): Future[Seq[UserTeamOrganization]] = {
    client.Users.getOrganizationsAndTeamsByName(userOrg.name).map(
      teams =>
        teams.map(team => UserTeamOrganization(userOrg.name, team))
    )
  }

  private def handleFormErrors(formWithErrors: Form[_]) = {
    Future.successful(Redirect(routes.Application.index()).flashing("error" -> buildFormValidationErrorMessage(formWithErrors)))
  }

  private def deleteUser(userDelete: UserDelete)(implicit request: Request[AnyContent]) = {
    userDelete match {
      case UserDelete(_, _, teamName) if teamName.nonEmpty => deleteUserFromTeam(userDelete)
      case _ => deleteUserFromOrganization(userDelete)
    }
  }

  private def deleteUserFromTeam(ud: UserDelete)
                                (implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Teams.deleteOrganizationsAndUsersByOrganizationAndTeamAndEmail(ud.orgName, ud.teamName, ud.email).map {
      response =>
        debug(s"Removed ${ud.email} from ${ud.teamName}@@${ud.orgName}")
        Redirect(routes.Teams.team(ud.orgName, ud.teamName))
          .flashing("success" -> Messages("cave.team.removeUser.success", ud.email, ud.teamName, ud.orgName))
    } recover {
      case error: FailedRequest if error.responseCode == BAD_REQUEST =>
        Logger.debug("Unable to remove user from team. Bad Request", error)
        Redirect(routes.Organizations.organization(ud.orgName))
          .flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
    }
  }

  private def deleteUserFromOrganization(userDelete: UserDelete)
                                        (implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Organizations.deleteUsersByNameAndEmail(userDelete.orgName, userDelete.email).map {
      response =>
        debug(s"Removed ${userDelete.email} from ${userDelete.orgName}")
        Redirect(routes.Organizations.organization(userDelete.orgName))
          .flashing("success" -> Messages("cave.organizations.removeUser.success", userDelete.email, userDelete.orgName))
    } recover {
      case error: FailedRequest if error.responseCode == BAD_REQUEST =>
        Logger.debug("Unable to remove user from organization. Bad Request", error)
        Redirect(routes.Organizations.organization(userDelete.orgName))
          .flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
    }
  }
}

object Application extends Application