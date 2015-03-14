package controllers

import com.gilt.cavellc.Client
import com.gilt.cavellc.models.{Member, Organization, Role, Team}
import controllers.helpers.{CreateToken, AddUserData}
import controllers.helpers.CaveForms._
import play.api.Logger
import play.api.i18n.Messages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TeamData(team: Team, role: Role, members: Seq[Member])

class Teams extends AbstractCaveController {

  def team(organizationName: String, teamName: String) = caveAsyncAction { implicit request =>
    withCaveClient { client =>

      val data = for {
        userOrganizations <- client.Users.getOrganizations()
        organization <- getOrganization(organizationName, client, userOrganizations)
        userTeams <- getUserTeams(userOrganizations, client)
        roleInOrganization <- Future.successful(userOrganizations.filter(_.name == organizationName).head.role)
        roleInTeam <- getRoleInTeam(organizationName, teamName, userTeams, roleInOrganization)
        teamsInOrganization <- getTeamsInOrganization(organizationName, roleInOrganization, client)
        team <- getSelectedTeam(organizationName, teamName, client, roleInTeam)
        membersInTeam <- getMembers(organizationName, teamName, client, organization, roleInTeam, team)
      } yield (organization, team, roleInTeam, membersInTeam.sortBy(_.user.firstName), userOrganizations.sortBy(_.name), userTeams.sortBy(_.userTeam.name), roleInOrganization)

      data map {
        case (Some(org), Some(team), roleInTeam, membersInTeam, userOrganizations, userTeams, roleInOrganization) =>
          val menuData = SideMenuData(userOrganizations, userTeams, Some(org), Some(team), Some(roleInOrganization))
          val teamData = TeamData(team, roleInTeam, membersInTeam)
          val addUserFormWithOrgAndTeam = addUserForm.fill(AddUserData(EMPTY_STRING, organizationName, teamName, EMPTY_STRING))
          val createTokenFormWithOrg = createTokenForm.fill(CreateToken(org.name, Some(team.name), EMPTY_STRING))

          Ok(views.html.teams.team(menuData, teamData, addUserFormWithOrgAndTeam, createTokenFormWithOrg))

        case _ =>
          Logger.warn(s"Team not found $teamName @ $organizationName")
          InternalServerError(views.html.errorpages.errorPage(Messages("cave.errors.5xx.general")))
      }
    }
  }

  private[controllers] def getMembers(organizationName: String, teamName: String, client: Client, organization: Option[Organization], role: Role, team: Option[Team]): Future[Seq[Member]] = {
    if (organization.isDefined && team.isDefined && !Seq(Role.Viewer, Role.Team).contains(role))
      client.Teams.getOrganizationsAndUsersByOrganizationAndTeam(organizationName, teamName).map(_.sortBy(_.user.firstName))
    else
      Future.successful(List.empty)
  }

  private[controllers] def getSelectedTeam(organizationName: String, teamName: String, client: Client, role: Role): Future[Option[Team]] = {
    if (!Seq(Role.Viewer, Role.Team).contains(role))
      client.Teams.getOrganizationsByOrganizationAndTeam(organizationName, teamName)
    else
      Future.successful(Some(Team(teamName, Seq.empty)))
  }
}
