package controllers

import com.cave.metrics.data._
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Alerts extends AbstractApiController {
  this: Controller =>

  import scala.concurrent.ExecutionContext.Implicits.global
  private val log = LogFactory.getLog("Alerts")

  final val LINK = "Link"

  def createOrganizationAlert(orgName: String) = MeteredAction(ServiceName, "create-organization-alert") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withOrganizationRole(user, org, Role.AtLeastMember) {
          withAlert(request.body) { alert =>
            if (alert.id.isDefined) {
              Future.successful(BadRequest("To update an existing alert, use the PATCH verb."))
            } else {
              alertManager.createOrganizationAlert(org, alert) match {
                case Success(Some(createdAlert)) =>
                  awsWrapper.createAlertNotification(Schedule(org.name, None, org.influxCluster, org.notificationUrl, createdAlert))
                  dataSink.sendMetrics(Seq(createKpiMetric(org.name, None, "create", createdAlert.id.get)))
                  Future.successful(Created(Json.toJson(createdAlert)).withHeaders(LOCATION -> makeLocation(org, createdAlert)))

                case Success(None) =>
                  Future.successful(BadRequest(s"Unable to parse Alert condition: ${alert.condition}."))

                case Failure(e) =>
                  log.warn(s"Error during createOrganizationAlert: ${e.getMessage}")
                  Future.successful(InternalServerError(InternalErrorMessage))
              }
            }
          }
        }
      }
    }
  }}

  def getOrganizationAlerts(orgName: String, limit: Int = 20, offset: Int = 0) = MeteredAction(ServiceName, "get-organization-alerts") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withOrganizationRole(user, org, Role.AtLeastMember) {
          alertManager.getOrganizationAlerts(org, limit + 1, offset) match {
            case Success(alertsAndOne) => buildResponse(org, None, alertsAndOne.take(limit), limit, offset, alertsAndOne.size > limit)
            case Failure(e) =>
              log.warn(s"Error during getOrganizationAlerts: ${e.getMessage}")
              Future.successful(InternalServerError(InternalErrorMessage))
          }
        }
      }
    }
  }}

  def getOrganizationAlert(orgName: String, alertId: String) = MeteredAction(ServiceName, "get-organization-alert") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withOrganizationRole(user, org, Role.AtLeastMember) {
          alertManager.getAlert(alertId) match {
            case Success(Some(alert)) =>
              Future.successful(Ok(Json.toJson(alert)).withHeaders(LOCATION -> makeLocation(org, alert)))

            case Success(None) => Future.successful(NotFound)
            case Failure(e) =>
              log.warn(s"Error during getAlert: ${e.getMessage}")
              Future.successful(InternalServerError(InternalErrorMessage))
          }
        }
      }
    }
  }}

  def updateOrganizationAlert(orgName: String, alertId: String) = MeteredAction(ServiceName, "update-organization-alert") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withOrganizationRole(user, org, Role.AtLeastMember) {
          alertManager.getAlert(alertId) match {
            case Success(Some(alert)) =>
              withAlertPatch(request.body) { alertPatch =>
                alertManager.updateAlert(alert, alertPatch) match {
                  case Success(Some(updatedAlert)) =>
                    awsWrapper.updateAlertNotification(Schedule(org.name, None, org.influxCluster, org.notificationUrl, updatedAlert))
                    if (alert.enabled != updatedAlert.enabled) {
                       dataSink.sendMetrics(Seq(createKpiMetric(org.name, None,
                         if (updatedAlert.enabled) "enable" else "disable",
                         updatedAlert.id.get)))
                    }
                    Future.successful(Ok(Json.toJson(updatedAlert)).withHeaders(LOCATION -> makeLocation(org, updatedAlert)))

                  case Success(None) => Future.successful(NotFound)
                  case Failure(e) =>
                    log.warn(s"Error during updateAlert: ${e.getMessage}")
                    Future.successful(InternalServerError(InternalErrorMessage))
                }
              }

            case Success(None) => Future.successful(NotFound)
            case Failure(e) =>
              log.warn(s"Error during getAlert: ${e.getMessage}")
              Future.successful(InternalServerError(InternalErrorMessage))
          }
        }
      }
    }
  }}

  def deleteOrganizationAlert(orgName: String, alertId: String) = MeteredAction(ServiceName, "delete-organization-alert") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withOrganizationRole(user, org, Role.AtLeastMember) {
          alertManager.deleteAlert(alertId) match {
            case Success(true) =>
              awsWrapper.deleteAlertNotification(alertId, org.name)
              dataSink.sendMetrics(Seq(createKpiMetric(org.name, None, "delete", alertId)))
              Future.successful(NoContent)

            case Success(false) => Future.successful(NotFound)
            case Failure(e) =>
              log.warn(s"Error during deleteAlert: ${e.getMessage}")
              Future.successful(InternalServerError(InternalErrorMessage))
          }
        }
      }
    }
  }}

  def createTeamAlert(orgName: String, teamName: String) = MeteredAction(ServiceName, "create-team-alert") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withTeam(org, teamName) { team =>
          withTeamRole(user, org, team, Role.AtLeastMember) {
            withAlert(request.body) { alert =>
              if (alert.id.isDefined) {
                Future.successful(BadRequest("To update an existing alert, use the PATCH verb."))
              } else {
                alertManager.createTeamAlert(org, team, alert) match {
                  case Success(Some(createdAlert)) =>
                    awsWrapper.createAlertNotification(Schedule(org.name, Some(team.name), team.influxCluster, org.notificationUrl, createdAlert))
                    dataSink.sendMetrics(Seq(createKpiMetric(org.name, Some(team.name), "create", createdAlert.id.get)))
                    Future.successful(
                      Created(Json.toJson(createdAlert)).withHeaders(LOCATION -> makeLocation(org, team, createdAlert)))

                  case Success(None) =>
                    Future.successful(BadRequest(s"Unable to parse Alert condition: ${alert.condition}."))

                  case Failure(e) =>
                    log.warn(s"Error during createTeamAlert: ${e.getMessage}")
                    Future.successful(InternalServerError(InternalErrorMessage))
                }
              }
            }
          }
        }
      }
    }
  }}

  def getTeamAlerts(orgName: String, teamName: String, limit: Int = 20, offset: Int = 0) = MeteredAction(ServiceName, "get-team-alerts") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withTeam(org, teamName) { team =>
          withTeamRole(user, org, team, Role.AtLeastMember) {
            alertManager.getTeamAlerts(org, team, limit + 1, offset) match {
              case Success(alerts) => buildResponse(org, Some(team), alerts.take(limit), limit, offset, alerts.size > limit)
              case Failure(e) =>
                log.warn(s"Error during getTeamAlerts: ${e.getMessage}")
                Future.successful(InternalServerError(InternalErrorMessage))
            }
          }
        }
      }
    }
  }}

  def getTeamAlert(orgName: String, teamName: String, alertId: String) = MeteredAction(ServiceName, "get-team-alert") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withTeam(org, teamName) { team =>
          withTeamRole(user, org, team, Role.AtLeastMember) {
            alertManager.getAlert(alertId) match {
              case Success(Some(alert)) =>
                Future.successful(Ok(Json.toJson(alert)).withHeaders(LOCATION -> makeLocation(org, team, alert)))

              case Success(None) => Future.successful(NotFound)
              case Failure(e) =>
                log.warn(s"Error during getAlert: ${e.getMessage}")
                Future.successful(InternalServerError(InternalErrorMessage))
            }
          }
        }
      }
    }
  }}

  def updateTeamAlert(orgName: String, teamName: String, alertId: String) = MeteredAction(ServiceName, "update-team-alert") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withTeam(org, teamName) { team =>
          withTeamRole(user, org, team, Role.AtLeastMember) {
            alertManager.getAlert(alertId) match {
              case Success(Some(alert)) =>
                withAlertPatch(request.body) { alertPatch =>
                  alertManager.updateAlert(alert, alertPatch) match {
                    case Success(Some(updatedAlert)) =>
                      awsWrapper.updateAlertNotification(Schedule(org.name, Some(team.name), team.influxCluster, org.notificationUrl, updatedAlert))
                      if (updatedAlert.enabled != alert.enabled)
                        dataSink.sendMetrics(Seq(createKpiMetric(org.name, Some(team.name),
                          if (updatedAlert.enabled) "enable" else "disable", updatedAlert.id.get)))
                      Future.successful(Ok(Json.toJson(updatedAlert)).withHeaders(LOCATION -> makeLocation(org, team, updatedAlert)))

                    case Success(None) => Future.successful(NotFound)
                    case Failure(e) =>
                      log.warn(s"Error during updateAlert: ${e.getMessage}")
                      Future.successful(InternalServerError(InternalErrorMessage))
                  }
                }

              case Success(None) => Future.successful(NotFound)
              case Failure(e) =>
                log.warn(s"Error during getAlert: ${e.getMessage}")
                Future.successful(InternalServerError(InternalErrorMessage))
            }
          }
        }
      }
    }
  }}

  def deleteTeamAlert(orgName: String, teamName: String, alertId: String) = MeteredAction(ServiceName, "delete-team-alert") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(orgName) { org =>
        withTeam(org, teamName) { team =>
          withTeamRole(user, org, team, Role.AtLeastMember) {
            alertManager.deleteAlert(alertId) match {
              case Success(true) =>
                awsWrapper.deleteAlertNotification(alertId, org.name)
                dataSink.sendMetrics(Seq(createKpiMetric(org.name, Some(team.name), "delete", alertId)))
                Future.successful(NoContent)

              case Success(false) => Future.successful(NotFound)
              case Failure(e) =>
                log.warn(s"Error during deleteAlert: ${e.getMessage}")
                Future.successful(InternalServerError(InternalErrorMessage))
            }
          }
        }
      }
    }
  }}

  private[this] def withAlert(json: JsValue)(block: Alert => Future[Result]): Future[Result] = {
    json.validate[Alert] match {
      case alert: JsSuccess[Alert] => block(alert.get)
      case error: JsError => Future.successful(BadRequest(s"Cannot parse alert configuration: ${error.errors}"))
    }
  }

  private[this] def withAlertPatch(json: JsValue)(block: AlertPatch => Future[Result]): Future[Result] = {
    json.validate[AlertPatch] match {
      case alertPatch: JsSuccess[AlertPatch] => block(alertPatch.get)
      case error: JsError => Future.successful(BadRequest(s"Cannot parse alert configuration: ${error.errors}"))
    }
  }

  private[this] def createPagingHeaders(org: Organization, team: Option[Team], limit: Int, offset: Int, hasNext: Boolean): Option[(String, String)] = {
    val teamSeg = team.map(t => s"teams/${t.name}/").getOrElse("")

    val nextOpt =
      if (hasNext) {
        val nextOffset = offset + limit
        Some(s"""<$baseUrl/organizations/${org.name}/${teamSeg}alerts?limit=$limit&offset=${nextOffset}>; rel="next"""")
      } else None

    val prevOpt =
      if (offset > 0) {
        val prevOffset = Math.max(offset - limit, 0)
        Some(s"""<$baseUrl/organizations/${org.name}/${teamSeg}alerts?limit=$limit&offset=${prevOffset}>; rel="prev"""")
      } else None

    val paging = List(nextOpt, prevOpt).flatten

    if (paging.nonEmpty)
      Some((LINK, paging.mkString(", ")))
    else
      None
  }

  private[this] def buildResponse(org: Organization, team: Option[Team], alerts: Seq[Alert], limit: Int, offset: Int, hasNext: Boolean) = {
    val alertsJson = Json.toJson(alerts)

    Future.successful {
      createPagingHeaders(org, team, limit, offset, hasNext).foldLeft(Ok(alertsJson)) { (ok, linkHeader) =>
        ok.withHeaders(linkHeader)
      }
    }
  }

  private[this] def createKpiMetric(organization: String, team: Option[String], operation: String, id: String): Metric = new Metric(
    "alertsOperations",
    new DateTime().getMillis / 1000,
    1,
    Map(
      "alert" -> id,
      "organization" -> organization,
      "team" -> team.getOrElse(""),
      "operation" -> operation,
      Metric.Organization -> "cave-kpi"
    )
  )
}

object Alerts extends Controller with Alerts
