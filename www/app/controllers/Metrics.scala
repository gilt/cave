package controllers

import com.gilt.cavellc.models.Role

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Metrics extends AbstractCaveController {

  def deleteTeamMetric(organizationName: String, teamName: String, metricName: String) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      for {
        deleteResult <- client.Teams.deleteOrganizationsAndMetricNamesByOrganizationNameAndTeamNameAndMetric(organizationName, teamName, metricName)
      } yield {
        // TODO: Will need to handle error scenarios (e.g., when server returns 404 or 500)
        Redirect(routes.Metrics.teamMetrics(organizationName, teamName))
      }
    }
  }

  def deleteOrganizationMetric(organizationName: String, metricName: String) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      for {
        deleteResult <- client.Organizations.deleteMetricNamesByNameAndMetric(organizationName, metricName)
      } yield {
        // TODO: Will need to handle error scenarios (e.g., when server returns 404 or 500)
        Redirect(routes.Metrics.organizationMetrics(organizationName))
      }
    }
  }

  def organizationMetrics(organizationName: String) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      val breadcrumb = ListMap("Home" -> routes.Application.index,
        organizationName -> routes.Organizations.organization(organizationName),
        "metrics" -> routes.Metrics.organizationMetrics(organizationName))
      for {
        metrics <- client.Metrics.getOrganizationsAndMetricNamesByOrganization(organizationName)
        role <- client.Users.getOrganizations().map(_.filter(_.name == organizationName).map(_.role).headOption.getOrElse(Role.Viewer))
      } yield Ok(views.html.metrics.metrics(metrics, organizationName, None, role, breadcrumb))
    }
  }

  def teamMetrics(organizationName: String, teamName: String) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      for {
        organizations <- client.Users.getOrganizations()
        metrics <- client.Metrics.getOrganizationsAndTeamsAndMetricNamesByOrganizationAndTeam(organizationName, teamName)
        roleInOrganization <- Future.successful(organizations.filter(_.name == organizationName).head.role)
        userTeams <- getUserTeams(organizations, client)
        role <- getRoleInTeam(organizationName, teamName, userTeams, roleInOrganization)
        breadcrumb <- Future.successful(ListMap(
          "Home" -> routes.Application.index,
          organizationName -> buildOrganizationUrlForBreadcrumb(organizationName, organizations),
          teamName -> routes.Teams.team(organizationName, teamName),
          "metrics" -> routes.Metrics.teamMetrics(organizationName, teamName))
        )
      } yield Ok(views.html.metrics.metrics(metrics, organizationName, Some(teamName), role, breadcrumb))
    }
  }
}

