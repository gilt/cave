package controllers

import com.cave.metrics.data.Role
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Internal extends AbstractApiController {
  this: Controller =>

  import scala.concurrent.ExecutionContext.Implicits.global

  private val log = LogFactory.getLog("Internal")
  final val Healthy = "healthy"
  final val Unhealthy = "unhealthy"

  def healthCheck() = MeteredAction(ServiceName, "healthCheck") { Action { implicit request =>
    Ok(if (dataManager.isHealthy) Healthy else Unhealthy)
  }}

  def version = MeteredAction(ServiceName, "version") { Action { request =>
    Ok(Json.toJson(Map("name" -> getClass.getPackage.getImplementationTitle, "version" -> getClass.getPackage.getImplementationVersion)))
  }}

  def reset = MeteredAction(ServiceName, "reset") { Action.async { request =>
    val now = new DateTime()
    dataManager.deleteExpiredSessionTokens(now) flatMap { _ =>
      dataManager.deleteExpiredConfirmationTokens(now) map { _ => Ok }
    }
  }}

  def status = MeteredAction(ServiceName, "status") { Action.async { request =>
    dataManager.getStatus() map { result =>
      Ok(Json.toJson(result))
    }
  }}

  def getOrganizationCluster(org: String) = MeteredAction(ServiceName, "get-organization-cluster") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          Future.successful(Ok(Json.obj("cluster" -> organization.influxCluster)))
        }
      }
    }
  }}

  def getTeamCluster(org: String, teamName: String) = MeteredAction(ServiceName, "get-team-cluster") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            Future.successful(Ok(Json.obj("cluster" -> team.influxCluster)))
          }
        }
      }
    }
  }}

  def updateOrganizationCluster(org: String) = MeteredAction(ServiceName, "update-organization-cluster") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          withClusterName(request.body) { cluster =>
            dataManager.updateOrganizationCluster(organization, cluster) match {
              case Success(Some(updatedOrg)) =>
                Future.successful(Ok(Json.toJson(updatedOrg)))

              case Success(None) => Future.successful(NotFound)
              case Failure(e) =>
                log.warn(s"Error during updateOrganizationCluster: ${e.getMessage}")
                Future.successful(InternalServerError(InternalErrorMessage))
            }
          }
        }
      }
    }
  }}

  def updateTeamCluster(org: String, teamName: String) = MeteredAction(ServiceName, "update-team-cluster") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            withClusterName(request.body) { cluster =>
              dataManager.updateTeamCluster(organization, team, cluster) match {
                case Success(Some(updatedTeam)) =>
                  Future.successful(Ok(Json.toJson(updatedTeam)))

                case Success(None) => Future.successful(NotFound)
                case Failure(e) =>
                  log.warn(s"Error during updateTeamCluster: ${e.getMessage}")
                  Future.successful(InternalServerError(InternalErrorMessage))
              }
            }
          }
        }
      }
    }
  }}

  private[this] def withClusterName(json: JsValue)(block: Option[String] => Future[Result]): Future[Result] =
    (json \ "cluster").validate[String] match {
      case string: JsSuccess[String] => block(Some(string.get))
      case error: JsError => block(None)
    }
}

object Internal extends Controller with Internal