package controllers

import com.gilt.cavellc.models._
import com.gilt.cavellc.Client
import com.gilt.cavellc.errors.FailedRequest
import controllers.helpers.CaveForms._
import controllers.helpers._
import play.api.Logger
import play.api.i18n.Messages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class OrganizationData(members: Seq[Member], role: Role, teams: Seq[UserTeam], organization: Organization)

class Organizations extends AbstractCaveController {

  def organization(organizationName: String) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      val data = for {
        userOrganizations <- client.Users.getOrganizations()
        selectedOrganization <- getOrganization(organizationName, client, userOrganizations)
        userTeams <- getUserTeams(userOrganizations, client)
        roleInOrganization <- Future.successful(userOrganizations.filter(_.name == organizationName).head.role)
        teamsInOrganization <- getTeamsInOrganization(organizationName, roleInOrganization, client)
        membersInOrganization <- getMembers(organizationName, client, selectedOrganization, roleInOrganization)
      } yield (teamsInOrganization.sortBy(_.name), selectedOrganization, membersInOrganization.sortBy(_.user.firstName), userOrganizations.sortBy(_.name), userTeams.sortBy(_.userTeam.name), roleInOrganization)

      data map {
        case (teams, Some(org: Organization), members, userOrganizations, userTeams, roleInOrganization) if roleInOrganization != Role.Team =>
          val updateOrgForm = organizationForm.fill(NewOrganization(org.name, org.email, org.notificationUrl))
          val addUserFormWithOrg = addUserForm.fill(AddUserData(EMPTY_STRING, org.name, EMPTY_STRING, EMPTY_STRING))
          val createTeamFormWithOrg = createTeamForm.fill(CreateTeam(EMPTY_STRING, org.name))
          val createTokenFormWithOrg = createTokenForm.fill(CreateToken(org.name, None, EMPTY_STRING))
          val organizationData = OrganizationData(members, roleInOrganization, teams, org)
          val menuData = SideMenuData(userOrganizations, userTeams, Some(org), None, Some(roleInOrganization))
          Ok(views.html.organizations.organizations(organizationData, menuData, addUserFormWithOrg, updateOrgForm,
            createTokenFormWithOrg, createTeamFormWithOrg))

        case _ =>
          Logger.warn(s"Organization not found $organizationName")
          InternalServerError(views.html.errorpages.errorPage(Messages("cave.errors.5xx.fetchOrganization", organizationName)))
      }
    }
  }


  def createOrganization = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      organizationForm.bindFromRequest().fold(
        formWithErrors => Future.successful(Redirect(routes.Application.index).flashing("error" -> buildFormValidationErrorMessage(formWithErrors))),
        organization =>
          client.Organizations.post(organization.name, organization.email, organization.notificationUrl) map {
            case org =>
              debug(s"Created ${organization.name}")
              Redirect(routes.Organizations.organization(organization.name)).flashing("success" -> Messages("cave.dashboard.createNewOrganization.modal.success", organization.name))
          } recover {
            case error: FailedRequest if error.responseCode == BAD_REQUEST =>
              Logger.debug("Unable to update organization. Bad Request", error)
              Redirect(routes.Application.index).flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
          }
      )
    }
  }

  def handleCreateTeam = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      createTeamForm.bindFromRequest().fold(
        formWithErrors => Future.successful(Redirect(routes.Application.index).flashing("error" -> buildFormValidationErrorMessage(formWithErrors))),
        createTeam =>
          client.Teams.postOrganizationsByOrg(createTeam.orgName, createTeam.teamName) map {
            case org =>
              debug(s"Created Team ${createTeam.teamName}")
              Redirect(routes.Organizations.organization(createTeam.orgName)).flashing("success" -> Messages("cave.dashboard.createNewTeam.modal.success", createTeam.teamName))
          } recover {
            case error: FailedRequest if error.responseCode == BAD_REQUEST =>
              Logger.debug("Unable to update create team. Bad Request", error)
              Redirect(routes.Application.index).flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
          }
      )
    }
  }

  def updateOrganization = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      organizationForm.bindFromRequest().fold(
        formWithErrors => Future.successful(Redirect(routes.Application.index).flashing("error" -> buildFormValidationErrorMessage(formWithErrors))),
        organization => {
          debug(s"Updating organization $organization")
          client.Organizations.patchByName(organization.name, Some(organization.email), Some(organization.notificationUrl)) map {
            case org => Redirect(routes.Organizations.organization(organization.name)).flashing("success" -> Messages("cave.dashboard.updateOrganization.modal.success", organization.name))
          } recover {
            case error: FailedRequest if error.responseCode == BAD_REQUEST =>
              Logger.debug("Unable to create organization. Bad Request", error)
              Redirect(routes.Application.index).flashing("error" -> Messages("cave.errors.api.validationError", error.getMessage))
          }
        }
      )
    }
  }

  private def getMembers(organizationName: String, client: Client, selectedOrganization: Option[Organization], roleInOrganization: Role): Future[Seq[Member]] = {
    if (selectedOrganization.isDefined && !Seq(Role.Viewer, Role.Team).contains(roleInOrganization)) client.Organizations.getUsersByName(organizationName) else Future.successful(List.empty)
  }
}
