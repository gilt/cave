package controllers

import com.cave.metrics.data._
import org.apache.commons.logging.LogFactory
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait Organizations extends AbstractApiController {
  this: Controller =>

  import scala.concurrent.ExecutionContext.Implicits.global
  private val log = LogFactory.getLog("Organizations")

  def createOrganization() = MeteredAction(ServiceName, "create-organization") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      request.body.asOpt[Organization] match {
        case Some(org) =>
          if (isValid(org.name)) {
            dataManager.createOrganization(user, Organization(org.id, org.name, org.email, org.notificationUrl, Some(List(Token.createToken(Token.DefaultName))))) match {
              case Success(Some(newOrg)) =>
                val database = Metric.createDbName(newOrg.name, None)
                val (client, context) = influxClientFactory.getClient(newOrg.influxCluster)
                client.createDatabase(database)(context) map {
                  case Success(_) =>
                    awsWrapper.createOrganizationNotification(newOrg)
                    Created(Json.toJson(newOrg)).withHeaders(LOCATION -> makeLocation(newOrg))

                  case Failure(e) =>
                    dataManager.deleteOrganization(org.name)
                    log.warn(s"Error during createOrganization: ${e.getMessage}")
                    InternalServerError(InternalErrorMessage)
                }

              case Success(None) => Future.successful(Conflict(s"Organization '${org.name}' already exists."))
              case Failure(e) => Future.successful(InternalServerError(e.getMessage))
            }
          } else Future.successful(BadRequest(s"Invalid organization name ${org.name}."))
        case None => Future.successful(BadRequest("Cannot parse JSON as Organization."))
      }
    }
  }}

  def getOrganization(org: String) = MeteredAction(ServiceName, "get-organization") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AtLeastMember) {
          Future.successful(Ok(Json.toJson(organization)).withHeaders(LOCATION -> makeLocation(organization)))
        }
      }
    }
  }}

  def getUsers(org: String) = MeteredAction(ServiceName, "get-organization-users") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AtLeastMember) {
          dataManager.getUsersForOrganization(organization) map { list =>
            Ok(Json.toJson(list))
          } recover {
            case t: Throwable =>
              log.warn(s"Error during getUsersForOrganization: ${t.getMessage}")
              InternalServerError(InternalErrorMessage)
          }
        }
      }
    }
  }}

  def addUser(org: String) = MeteredAction(ServiceName, "add-organization-user") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          withNewUser(request.body) { case (email, role) =>
            dataManager.getUserByEmail(email) flatMap {
              case Some(newUser) =>
                dataManager.addUserToOrganization(newUser, organization, role) map { result =>
                  if (result) Accepted else Conflict
                } recover {
                  case NonFatal(e) =>
                    log.warn(s"Error during addUserToOrganization: ${e.getMessage}")
                    InternalServerError(InternalErrorMessage)
                }
              case None =>
                log.warn(s"No CAVE user found with email $email.")
                mailService.sendAttemptedOrganizationAdd(email, organization, user)
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
  }}

  def modifyUser(org: String, email: String) = MeteredAction(ServiceName, "modify-organization-user") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          if (user.email == email) {
            Future.successful(BadRequest("Cannot modify self."))
          } else {
            withRole(request.body) { role =>
              dataManager.getUserByEmail(email) flatMap {
                case Some(orgUser) =>
                  dataManager.changeOrganizationRole(orgUser, organization, role) map { _ =>
                    Accepted
                  } recover {
                    case NonFatal(e) =>
                      log.warn(s"Error during changeOrganizationRole: ${e.getMessage}")
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

  def removeUser(org: String, email: String) = MeteredAction(ServiceName, "remove-organization-user") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          if (user.email == email) {
            Future.successful(BadRequest("Cannot remove self."))
          } else {
            dataManager.getUserByEmail(email) flatMap {
              case Some(orgUser) =>
                dataManager.deleteUserFromOrganization(orgUser, organization) map { _ =>
                  NoContent
                } recover {
                  case NonFatal(e) =>
                    log.warn(s"Error during deleteUserFromOrganization: ${e.getMessage}")
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

  def updateOrganization(org: String) = MeteredAction(ServiceName, "update-organization") { Action.async(parse.json) { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          request.body.asOpt[OrganizationPatch] match {
            case Some(OrganizationPatch(None, None)) =>
              Future.successful(BadRequest("No fields specified for update."))

            case Some(organizationPatch) =>
              dataManager.updateOrganization(organization, organizationPatch) match {
                case Success(Some(updatedOrg)) =>
                  if (organization.notificationUrl != updatedOrg.notificationUrl) {
                    awsWrapper.updateOrganizationNotification(updatedOrg)
                  }
                  Future.successful(Ok(Json.toJson(updatedOrg)))

                case Success(None) => Future.successful(NotFound)
                case Failure(e) =>
                  log.warn(s"Error during updateOrganization: ${e.getMessage}")
                  Future.successful(InternalServerError(InternalErrorMessage))
              }
            case None => Future.successful(BadRequest("Failed to parse a patch element from request body."))
          }
        }
      }
    }
  }}

  def deleteOrganization(org: String) = MeteredAction(ServiceName, "delete-organization") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          dataManager.getTeams(organization) match {
            case Success(teams) =>
              if (teams.size > 0)
                Future.successful(BadRequest("Cannot delete account with teams attached. Delete teams first."))
              else
                dataManager.deleteOrganization(organization.name) match {
                  case Success(true) =>
                    awsWrapper.deleteOrganizationNotification(organization.name)
                    Future.successful(NoContent)

                  case Success(false) => Future.successful(NotFound)
                  case Failure(e) =>
                    log.warn(s"Error during deleteOrganization: ${e.getMessage}")
                    Future.successful(InternalServerError(InternalErrorMessage))
                }
            case Failure(e) =>
              log.warn(s"Error during getTeams: ${e.getMessage}")
              Future.successful(InternalServerError(InternalErrorMessage))
          }
        }
      }
    }
  }}
}

object Organizations extends Controller with Organizations