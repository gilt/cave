package controllers

import org.apache.commons.logging.LogFactory
import play.api.mvc._
import play.api.libs.json.{Json, JsError, JsSuccess, JsValue}
import scala.concurrent.Future
import scala.util.{Failure, Success}
import com.cave.metrics.data.{Role, Token}

trait Tokens extends AbstractApiController {
  this: Controller =>

  import scala.concurrent.ExecutionContext.Implicits.global
  private val log = LogFactory.getLog("Tokens")

  def createOrganizationToken(org: String) = MeteredAction(ServiceName, "create-organization-token") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          if (organization.tokens.get.size < maxTokens) {
            withDescription(request.body) { description =>
              dataManager.addOrganizationToken(organization, Token.createToken(description)) match {
                case Success(newToken) => Future.successful(Created(Json.toJson(newToken)).withHeaders(LOCATION -> makeLocation(organization, newToken)))
                case Failure(e) =>
                  log.warn(s"Error during addOrganizationToken: ${e.getMessage}")
                  Future.successful(InternalServerError(InternalErrorMessage))
              }
            }
          } else Future.successful(BadRequest("Too many tokens for organization."))
        }
      }
    }
  }}

  def getOrganizationTokens(org: String) = MeteredAction(ServiceName, "get-organization-tokens") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          Future.successful(Ok(Json.toJson(organization.tokens)))
        }
      }
    }
  }}

  def deleteOrganizationToken(org: String, id: String) = MeteredAction(ServiceName, "delete-organization-token") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          if (organization.tokens.get.size > 1) {
            dataManager.deleteToken(id) match {
              case Success(true) => Future.successful(NoContent)
              case Success(false) => Future.successful(NotFound)
              case Failure(e) =>
                log.warn(s"Error during addOrganizationToken: ${e.getMessage}")
                Future.successful(InternalServerError(InternalErrorMessage))
            }
          } else Future.successful(BadRequest("Cannot delete the last organization token."))
        }
      }
    }
  }}

  def createTeamToken(org: String, teamName: String) = MeteredAction(ServiceName, "create-team-token") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            if (team.tokens.get.size < maxTokens) {
              withDescription(request.body) { description =>
                dataManager.addTeamToken(organization, team, Token.createToken(description)) match {
                  case Success(newToken) =>
                    Future.successful(Created(Json.toJson(newToken)).withHeaders(LOCATION -> makeLocation(organization, team, newToken)))
                  case Failure(e) =>
                    log.warn(s"Error during addOrganizationToken: ${e.getMessage}")
                    Future.successful(InternalServerError(InternalErrorMessage))
                }
              }
            } else Future.successful(BadRequest("Too many tokens for team."))
          }
        }
      }
    }
  }}

  def getTeamTokens(org: String, teamName: String) = MeteredAction(ServiceName, "get-team-tokens") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            Future.successful(Ok(Json.toJson(team.tokens)))
          }
        }
      }
    }
  }}

  def deleteTeamToken(org: String, teamName: String, id: String) = MeteredAction(ServiceName, "delete-team-token") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            if (team.tokens.get.size > 1) {
              dataManager.deleteToken(id) match {
                case Success(true) => Future.successful(NoContent)
                case Success(false) => Future.successful(NotFound)
                case Failure(e) =>
                  log.warn(s"Error during addOrganizationToken: ${e.getMessage}")
                  Future.successful(InternalServerError(InternalErrorMessage))
              }
            } else Future.successful(BadRequest("Cannot delete the last team token."))
          }
        }
      }
    }
  }}

  private[this] def withDescription(json: JsValue)(block: String => Future[Result]): Future[Result] =
    (json \ "description").validate[String] match {
      case string: JsSuccess[String] => block(string.get)
      case error: JsError => Future.successful(BadRequest("Cannot parse request body: description is missing."))
    }
}

object Tokens extends Controller with Tokens