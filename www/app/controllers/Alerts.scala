package controllers

import com.gilt.cavellc.errors.FailedRequest
import com.gilt.cavellc.models.{Alert, Role, UserOrganization}
import controllers.helpers.CaveForms._
import controllers.helpers._
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Call, Request, Result}

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Paging(next: Option[Call], prev: Option[Call])

class Alerts extends AbstractCaveController {

  val pagerduty_service_api_key = "pagerduty_service_api_key"

  def disableTeamAlert(organizationName: String, teamName: String, alertId: String) = caveAsyncAction { implicit request =>
    changeTeamAlertStatus(organizationName, teamName, alertId, false)
  }

  def enableTeamAlert(organizationName: String, teamName: String, alertId: String) = caveAsyncAction { implicit request =>
    changeTeamAlertStatus(organizationName, teamName, alertId, true)
  }

  def disableOrganizationAlert(organizationName: String, alertId: String) = caveAsyncAction { implicit request =>
    changeOrganizationAlertStatus(organizationName, alertId, false)
  }

  def enableOrganizationAlert(organizationName: String, alertId: String) = caveAsyncAction { implicit request =>
    changeOrganizationAlertStatus(organizationName, alertId, true)
  }

  def removeAlert = caveAsyncAction { implicit request =>
    deleteAlertForm.bindFromRequest().fold(handleFormError, handleAlertDeletion)
  }

  def updateAlert = caveAsyncAction { implicit request =>
    editAlertForm.bindFromRequest().fold(handleFormError, handleEditAlert)
  }

  private def handleEditAlert(alert: EditAlert)(implicit request: Request[AnyContent]): Future[Result] = withCaveClient { client =>
    println("Alert: " + alert)
    alert.team match {
      case Some(team) => client.Alerts.patchOrganizationsAndTeamsByOrganizationAndTeamAndId(
        alert.organization, team, alert.alertId, Some(alert.description), Some(alert.status), Some(alert.period), Some(alert.handbookUrl),
        alert.routing.map(key => Map(pagerduty_service_api_key -> key))) map { result =>
        Redirect(routes.Alerts.teamAlert(alert.organization, team, alert.alertId)).flashing("success" -> Messages("cave.alerts.update.success"))
      }
      case _ => client.Alerts.patchOrganizationsByOrganizationAndId(
        alert.organization, alert.alertId, Some(alert.description), Some(alert.status), Some(alert.period), Some(alert.handbookUrl),
        alert.routing.map(key => Map(pagerduty_service_api_key -> key))) map { result =>
        Redirect(routes.Alerts.organizationAlert(alert.organization, alert.alertId)).flashing("success" -> Messages("cave.alerts.update.success"))
      }
    }
  }

  private def handleAlertDeletion(deleteAlert: DeleteAlert)(implicit request: Request[AnyContent]) = {
    deleteAlert.team match {
      case Some(team) => deleteTeamAlert(deleteAlert, team)
      case _ => deleteOrganizationAlert(deleteAlert)
    }
  }

  private def deleteOrganizationAlert(da: DeleteAlert)(implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Alerts.deleteOrganizationsByOrganizationAndId(da.organization, da.alertId) map { result =>
      Redirect(routes.Alerts.organizationAlerts(da.organization)).flashing("success" -> Messages("cave.alerts.delete.success"))
    }
  }

  private def deleteTeamAlert(da: DeleteAlert, team: String)(implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Alerts.deleteOrganizationsAndTeamsByOrganizationAndTeamAndId(da.organization, team, da.alertId) map { result =>
      Redirect(routes.Alerts.teamAlerts(da.organization, team)).flashing("success" -> Messages("cave.alerts.delete.success"))
    }
  }

  def teamAlert(organizationName: String, teamName: String, alertId: String) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      for {
        userOrganizations <- client.Users.getOrganizations()
        alert <- client.Alerts.getOrganizationsAndTeamsByOrganizationAndTeamAndId(organizationName, teamName, alertId)
      } yield alert match {
        case Some(a) => renderAlert(a, organizationName, Some(teamName), userOrganizations)
        case _ => NotFound(views.html.errorpages.pageNotFound(request.path))
      }
    }
  }

  def organizationAlert(organizationName: String, alertId: String) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      for {
        userOrganizations <- client.Users.getOrganizations()
        alert <- client.Alerts.getOrganizationsByOrganizationAndId(organizationName, alertId)
      } yield alert match {
        case Some(a) => renderAlert(a, organizationName, None, userOrganizations)
        case _ => NotFound(views.html.errorpages.pageNotFound(request.path))
      }
    }
  }

  def organizationAlerts(organizationName: String, limit: Int = 20, offset: Int = 0) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      val breadcrumb = ListMap("Home" -> routes.Application.index,
        organizationName -> routes.Organizations.organization(organizationName),
        "alerts" -> routes.Metrics.organizationMetrics(organizationName))

      for {
        alertsAndOne <- client.Alerts.getOrganizationsByOrganization(organizationName, Some(limit + 1), Some(offset))
      } yield {
        val alertForm = createAlertForm.fill(CreateAlert(organizationName, EMPTY_STRING, false, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, None))
        val paging = Paging(
          next = if (alertsAndOne.size > limit) Some(routes.Alerts.organizationAlerts(organizationName, limit, offset + limit)) else None,
          prev = if (offset > 0) Some(routes.Alerts.organizationAlerts(organizationName, limit, Math.max(offset - limit, 0))) else None
        )
        Ok(views.html.alerts.alerts(alertsAndOne.take(limit).sortBy(!_.enabled), organizationName, None, paging, breadcrumb, alertForm))
      }
    }
  }

  def teamAlerts(organizationName: String, teamName: String, limit: Integer = 20, offset: Integer = 0) = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      for {
        organizations <- client.Users.getOrganizations()
        alertsAndOne <- client.Alerts.getOrganizationsAndTeamsByOrganizationAndTeam(organizationName, teamName, Some(limit + 1), Some(offset))
        breadcrumb <- buildBreadcrumb(organizationName, teamName, organizations)
      } yield {
        val alertForm = createAlertForm.fill(CreateAlert(organizationName, teamName, false, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, None))
        val paging = Paging(
          next = if (alertsAndOne.size > limit) Some(routes.Alerts.teamAlerts(organizationName, teamName, limit, offset + limit)) else None,
          prev = if (offset > 0) Some(routes.Alerts.teamAlerts(organizationName, teamName, limit, Math.max(offset - limit, 0))) else None
        )
        Ok(views.html.alerts.alerts(alertsAndOne.take(limit).sortBy(!_.enabled), organizationName, Some(teamName), paging, breadcrumb, alertForm))
      }
    }
  }

  private def buildBreadcrumb(organizationName: String, teamName: String, organizations: Seq[UserOrganization]): Future[ListMap[String, _]] = {
    Future.successful(ListMap(
      "Home" -> routes.Application.index,
      organizationName -> buildOrganizationUrlForBreadcrumb(organizationName, organizations),
      teamName -> routes.Teams.team(organizationName, teamName),
      "alerts" -> routes.Metrics.teamMetrics(organizationName, teamName))
    )
  }

  def create = caveAsyncAction { implicit request =>
    createAlertForm.bindFromRequest().fold(
      formWithErrors => Future.successful(Redirect(routes.Application.index()).flashing("error" -> buildFormValidationErrorMessage(formWithErrors))),
      alert => handleAlertCreation(alert)
    ) .recover {
      case e: FailedRequest if e.responseCode == BAD_REQUEST =>
        Logger.debug(s"Unable to create alert. ${e.message}", e)
        Redirect(routes.Application.index()).flashing("error" -> Messages("cave.alerts.create.error", s"<pre>${e.getMessage}</pre>"))
    }
  }

  private def changeOrganizationAlertStatus(organizationName: String, alertId: String, enabled: Boolean)
                                           (implicit request: Request[AnyContent]): Future[Result] = withCaveClient { client =>
    client.Alerts.patchOrganizationsByOrganizationAndId(organizationName, alertId, enabled = Some(enabled)) map { alert =>
      Redirect(routes.Alerts.organizationAlert(organizationName, alert.id.getOrElse(""))).flashing("success" -> Messages("cave.alerts.update.success"))
    }
  }

  private def changeTeamAlertStatus(organizationName: String, teamName: String, alertId: String, enabled: Boolean)
                                   (implicit request: Request[AnyContent]): Future[Result] = withCaveClient { client =>
    client.Alerts.patchOrganizationsAndTeamsByOrganizationAndTeamAndId(organizationName, teamName, alertId, enabled = Some(enabled)) map { alert =>
      Redirect(routes.Alerts.teamAlert(organizationName, teamName, alert.id.getOrElse(""))).flashing("success" -> Messages("cave.alerts.update.success"))
    }
  }

  private def handleAlertCreation(alert: CreateAlert)
                                 (implicit request: Request[AnyContent]) = alert match {
    case CreateAlert(_, team, _, _, _, _, _, _) if team.nonEmpty => createTeamAlert(alert)
    case _ => createOrganizationAlert(alert)
  }

  private def createTeamAlert(alert: CreateAlert)
                             (implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Alerts.postOrganizationsAndTeamsByOrganizationAndTeam(alert.organization, alert.team, alert.description,
      alert.status, alert.period, alert.condition, Some(alert.handbookUrl), alert.routing.map(p => Map(pagerduty_service_api_key -> p))) map {
      response =>
        debug(s"Created Alert: $alert")
        Redirect(routes.Alerts.teamAlerts(alert.organization, alert.team)).flashing("success" -> Messages("cave.alerts.create.success"))
    }
  }

  private def createOrganizationAlert(alert: CreateAlert)
                                     (implicit request: Request[AnyContent]) = withCaveClient { client =>
    client.Alerts.postOrganizationsByOrganization(alert.organization, alert.description,
      alert.status, alert.period, alert.condition, Some(alert.handbookUrl), alert.routing.map(p => Map(pagerduty_service_api_key -> p))) map {
      response =>
        debug(s"Created Alert: $alert")
        Redirect(routes.Alerts.organizationAlerts(alert.organization)).flashing("success" -> Messages("cave.alerts.create.success"))
    }
  }

  private def renderAlert(alert: Alert, organizationName: String, teamName: Option[String], userOrganizations: Seq[UserOrganization])
                         (implicit request: Request[AnyContent]): Result = {
    val breadcrumb = teamName match {
      case Some(team) => ListMap("Home" -> routes.Application.index,
        organizationName -> (if (userOrganizations.exists(o => o.name == organizationName && o.role != Role.Team)) routes.Organizations.organization(organizationName) else ""),
        team -> routes.Teams.team(organizationName, team),
        "alerts" -> routes.Alerts.teamAlerts(organizationName, team),
        alert.id.getOrElse("alert") -> routes.Alerts.organizationAlert(organizationName, alert.id.getOrElse("ID")))
      case _ => ListMap("Home" -> routes.Application.index,
        organizationName -> routes.Organizations.organization(organizationName),
        "alerts" -> routes.Alerts.organizationAlerts(organizationName),
        alert.id.getOrElse("alert") -> routes.Alerts.organizationAlert(organizationName, alert.id.getOrElse("ID")))

    }
    val routing = alert.routing.get(pagerduty_service_api_key)
    val editForm = editAlertForm.fill(EditAlert(alert.id.getOrElse(""), organizationName, teamName, alert.enabled,
      alert.description, alert.period, alert.handbookUrl.getOrElse(""), routing, Some(alert.condition)))
    val createForm = createAlertForm.fill(CreateAlert(organizationName, teamName.getOrElse(""), alert.enabled, alert.description,
      alert.period, alert.condition, alert.handbookUrl.getOrElse(""), alert.routing.get(pagerduty_service_api_key)))
    val deleteForm = deleteAlertForm.fill(DeleteAlert(organizationName, teamName, alert.id.getOrElse("")))
    Ok(views.html.alerts.alert(alert, organizationName, breadcrumb, deleteForm, editForm, createForm, teamName))
  }
}
