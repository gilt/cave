package controllers

import org.apache.commons.logging.LogFactory
import play.api.mvc._
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}
import com.cave.metrics.data.{Role, Token, Metric, Team}

trait Teams extends AbstractApiController {
  this: Controller =>

  import scala.concurrent.ExecutionContext.Implicits.global
  private val log = LogFactory.getLog("Teams")

  def getTeams(org: String) = MeteredAction(ServiceName, "get-organization-teams") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AtLeastMember) {
          dataManager.getTeams(organization) match {
            case Success(teams) =>
              Future.successful(Ok(Json.toJson(teams)))

            case Failure(e) =>
              log.warn(s"Error during getTeams: ${e.getMessage}")
              Future.successful(InternalServerError(InternalErrorMessage))
          }
        }
      }
    }
  }}

  def createTeam(org: String) = MeteredAction(ServiceName, "create-team") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          request.body.asOpt[Team] match {
            case Some(team) =>
              if (isValid(team.name)) {
                dataManager.createTeam(organization, Team(team.id, team.name, Some(List(Token.createToken(Token.DefaultName))))) match {
                  case Success(Some(newTeam)) =>
                    val (client, context) = influxClientFactory.getClient(newTeam.influxCluster)
                    client.createDatabase(Metric.createDbName(organization.name, Some(team.name)))(context) map {
                      case Success(_) =>
                        Created(Json.toJson(newTeam)).withHeaders(LOCATION -> makeLocation(organization, newTeam))

                      case Failure(e) =>
                        dataManager.deleteTeam(organization, team.name)
                        InternalServerError(InternalErrorMessage)
                    }


                  case Success(None) => Future.successful(Conflict(s"Team '${team.name}' already exists."))
                  case Failure(e) =>
                    log.warn(s"Error during createTeam: ${e.getMessage}")
                    Future.successful(InternalServerError(InternalErrorMessage))
                }
              } else Future.successful(BadRequest(s"Invalid team name ${team.name}."))
            case None => Future.successful(BadRequest("Cannot parse JSON as Team."))
          }
        }
      }
    }
  }}

  def getTeam(org: String, teamName: String) = MeteredAction(ServiceName, "get-team") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AtLeastMember) {
            Future.successful(Ok(Json.toJson(team)))
          }
        }
      }
    }
  }}

  def getUsers(org: String, teamName: String) = MeteredAction(ServiceName, "get-team-users") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AtLeastMember) {
            dataManager.getUsersForTeam(team) map { list =>
              Ok(Json.toJson(list))
            } recover {
              case NonFatal(e) =>
                log.warn(s"Error during getUsersForTeam: ${e.getMessage}")
                InternalServerError(InternalErrorMessage)
            }
          }
        }
      }
    }
  }}

  def addUser(org: String, teamName: String) = MeteredAction(ServiceName, "add-team-user") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            withNewUser(request.body) { case (email, role) =>
              dataManager.getUserByEmail(email) flatMap {
                case Some(newUser) =>
                  dataManager.addUserToTeam(newUser, team, role) map { result =>
                    if (result) Accepted else Conflict
                  } recover {
                    case NonFatal(e) =>
                      log.warn(s"Error during addUserToTeam: ${e.getMessage}")
                      InternalServerError(InternalErrorMessage)
                  }

                case None =>
                  log.warn(s"No CAVE user found with email $email.")
                  mailService.sendAttemptedTeamAdd(email, organization, team, user)
                  Future.successful(Accepted)
              } recover {
                case NonFatal(e) =>
                  log.warn(s"Error during getUserByEmail: ${e.getMessage}")
                  InternalServerError(InternalErrorMessage)
              }
            }
          }
        }
      }
    }
  }}

  def modifyUser(org: String, teamName: String, email: String) = MeteredAction(ServiceName, "modify-team-user") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            withRole(request.body) { role =>
              dataManager.getUserByEmail(email) flatMap {
                case Some(teamUser) =>
                  dataManager.changeTeamRole(teamUser, team, role) map { _ =>
                    Accepted
                  } recover {
                    case NonFatal(e) =>
                      log.warn(s"Error during changeTeamRole: ${e.getMessage}")
                      InternalServerError(InternalErrorMessage)
                  }

                case None =>
                  log.warn(s"No CAVE user found with email $email.")
                  Future.successful(Accepted)
              } recover {
                case NonFatal(e) =>
                  log.warn(s"Error during getUserByEmail: ${e.getMessage}")
                  InternalServerError(InternalErrorMessage)
              }
            }
          }
        }
      }
    }
  }}

  def removeUser(org: String, teamName: String, email: String) = MeteredAction(ServiceName, "remove-team-user") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            dataManager.getUserByEmail(email) flatMap {
              case Some(teamUser) =>
                dataManager.deleteUserFromTeam(teamUser, team) map { _ =>
                  NoContent
                } recover {
                  case NonFatal(e) =>
                    log.warn(s"Error during changeTeamRole: ${e.getMessage}")
                    InternalServerError(InternalErrorMessage)
                }

              case None =>
                log.warn(s"No CAVE user found with email $email.")
                Future.successful(NoContent)
            } recover {
              case NonFatal(e) =>
                log.warn(s"Error during getUserByEmail: ${e.getMessage}")
                InternalServerError(InternalErrorMessage)
            }
          }
        }
      }
    }
  }}

  def deleteTeam(org: String, teamName: String) = MeteredAction(ServiceName, "delete-team") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            dataManager.deleteTeam(organization, team.name) match {
              case Success(true) => Future.successful(NoContent)
              case Success(false) => Future.successful(NotFound)
              case Failure(e) =>
                log.warn(s"Error during deleteTeam: ${e.getMessage}")
                Future.successful(InternalServerError(InternalErrorMessage))
            }
          }
        }
      }
    }
  }}
}

object Teams extends Controller with Teams