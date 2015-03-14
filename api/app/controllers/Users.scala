package controllers

import java.util.UUID

import com.cave.metrics.data._
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import play.api.mvc.{Action, Controller, Result}

import scala.concurrent.Future

trait Users extends AbstractApiController {
  this: Controller =>

  import scala.concurrent.ExecutionContext.Implicits.global
  private val log = LogFactory.getLog("Users")

  def registerUser() = MeteredAction(ServiceName, "register-user") { Action.async(parse.json) { request =>
    withEmail(request.body) { email =>
      dataManager.getUserByEmail(email) flatMap {
        case Some(user) => mailService.sendAlreadyRegisteredEmail(user) map (_ => Accepted)

        case None =>
          dataManager.createConfirmationToken(createToken(email, isSignUp = true)) flatMap {
            case Some(token) => mailService.sendRegistrationEmail(email, token) map (_ => Accepted)
            case None => Future.successful(InternalServerError(InternalErrorMessage))
          } recover {
            case t: Throwable =>
              log.warn(s"Error during createConfirmationToken: ${t.getMessage}")
              InternalServerError(InternalErrorMessage)
          }
      } recover {
        case t: Throwable =>
          log.warn(s"Error during getUserByEmail: ${t.getMessage}")
          InternalServerError(InternalErrorMessage)
      }
    }
  }}

  def confirmUser() = MeteredAction(ServiceName, "confirm-user") { Action.async(parse.json) { request =>
    request.body.asOpt[UserConfirmation] match {
      case Some(userConfirmation) =>
        dataManager.getConfirmationTokenByUUID(userConfirmation.confirmationToken) flatMap {
          case Some(token) =>
            if (token.expirationTime.isBefore(now())) {
              Future.successful(BadRequest("Confirmation token has expired. Please register again."))
            } else {
              val info = passwordHelper.encryptPassword(userConfirmation.password)
              val newUser = User(None, userConfirmation.firstName, userConfirmation.lastName, token.email, info.hash, info.salt)

              dataManager.createUser(newUser) flatMap {
                case Some(user) =>
                  dataManager.deleteConfirmationToken(token.uuid) map { _ =>
                    mailService.sendWelcomeEmail(newUser)
                    Created(Json.toJson(user))
                  } recover {
                    case t: Throwable =>
                      log.warn(s"Error during deleteConfirmationToken: ${t.getMessage}")
                      InternalServerError(InternalErrorMessage)
                  }
                case None => Future.successful(InternalServerError(InternalErrorMessage))
              } recover {
                case t: Throwable =>
                  log.warn(s"Error during createUser: ${t.getMessage}")
                  InternalServerError(InternalErrorMessage)
              }
            }

          case None => Future.successful(BadRequest("Invalid confirmation token."))
        } recover {
          case t: Throwable =>
            log.warn(s"Error during getConfirmationTokenByUUID: ${t.getMessage}")
            InternalServerError(InternalErrorMessage)
        }
      case None =>
        Future.successful(BadRequest("Cannot parse JSON data: must have first_name, last_name, email, password and confirmation_token."))
    }
  }}

  def loginUser() = MeteredAction(ServiceName, "login-user") { Action.async(parse.json) { request =>
    withLoginRequest(request.body) { case (email, password) =>
      dataManager.getUserByEmail(email) flatMap {
        case None => Future.successful(BadRequest("Login failed."))

        case Some(user) =>
          if (passwordHelper.matchPassword(password, user.password, user.salt)) {
            val creationTime = now()
            val expirationTime = creationTime.plusMinutes(60)
            dataManager.createSessionToken(user.id.get, creationTime, expirationTime) map { t =>
              Ok(Json.obj(
                "token" -> t.token,
                "expires" -> ISODateTimeFormat.dateTimeNoMillis.print(expirationTime)
              ))
            } recover {
              case t: Throwable =>
                log.warn(s"Error during createSessionToken: ${t.getMessage}")
                InternalServerError(InternalErrorMessage)
            }
          } else {
            Future.successful(BadRequest("Login failed."))
          }
      } recover {
        case t: Throwable =>
          log.warn(s"Error during getUserByEmail: ${t.getMessage}")
          InternalServerError(InternalErrorMessage)
      }
    }
  }}

  def forgotPassword() = MeteredAction(ServiceName, "forgot-password") { Action.async(parse.json) { request =>
    withEmail(request.body) { email =>
      dataManager.getUserByEmail(email) flatMap {
        case Some(user) =>
          dataManager.createConfirmationToken(createToken(email, isSignUp = false)) flatMap {
            case Some(token) => mailService.sendForgotPasswordEmail(user, token) map { _ =>
              Accepted
            } recover {
              case t: Throwable =>
                log.warn(s"Error during sendForgotPasswordEmail: ${t.getMessage}")
                InternalServerError(InternalErrorMessage)
            }
            case _ => Future.successful(InternalServerError(InternalErrorMessage))
          } recover {
            case t: Throwable =>
              log.warn(s"Error during createConfirmationToken: ${t.getMessage}")
              InternalServerError(InternalErrorMessage)
          }

        case None =>
          Future.successful(Accepted)
      } recover {
        case t: Throwable =>
          log.warn(s"Error during getUserByEmail: ${t.getMessage}")
          InternalServerError(InternalErrorMessage)
      }
    }
  }}

  def resetPassword() = MeteredAction(ServiceName, "reset-password") { Action.async(parse.json) { request =>
    withPasswordResetRequest(request.body) { case (token, newPassword) =>
       dataManager.getConfirmationTokenByUUID(token) flatMap {
         case None => Future.successful(BadRequest("Invalid token."))

         case Some(confirmationToken) =>
           if (confirmationToken.expirationTime.isBefore(now())) {
             Future.successful(BadRequest("Confirmation token has expired."))
           } else {
             dataManager.getUserByEmail(confirmationToken.email) flatMap {
               case None => Future.successful(BadRequest("Invalid token."))

               case Some(user) =>
                 dataManager.updateUser(user, None, None, Some(passwordHelper.encryptPassword(newPassword))) flatMap {
                   case None => Future.successful(InternalServerError(InternalErrorMessage))
                   case Some(newUser) => mailService.sendPasswordResetEmail(user) map (_ => Ok)
                 } recover {
                   case t: Throwable =>
                     log.warn(s"Error during updatePasswordInfo: ${t.getMessage}")
                     InternalServerError(InternalErrorMessage)
                 }
             } recover {
               case t: Throwable =>
                 log.warn(s"Error during getUserByEmail: ${t.getMessage}")
                 InternalServerError(InternalErrorMessage)
             }
           }
       } recover {
         case t: Throwable =>
           log.warn(s"Error during getConfirmationTokenByUUID: ${t.getMessage}")
           InternalServerError(InternalErrorMessage)
       }
    }
  }}

  def getUser = MeteredAction(ServiceName, "get-user") { Action.async { request =>
    withUser(request.headers) { user =>
      Future.successful(Ok(Json.toJson(user)))
    }
  }}

  def updateUser() = MeteredAction(ServiceName, "update-user") { Action.async(parse.json) { request =>
    request.body.asOpt[UserPatch] match {
      case Some(UserPatch(None, None, None)) =>
        Future.successful(BadRequest("Cannot parse request body: should contain at least one of 'first_name', 'last_name', or 'password'."))

      case Some(patch) =>
        withUser(request.headers) { user =>
          dataManager.updateUser(user, patch.firstName, patch.lastName, patch.password map passwordHelper.encryptPassword) map {
            case Some(updatedUser) =>
              Ok(Json.toJson(updatedUser))

            case _ =>
              log.warn("Failed to update user.")
              InternalServerError(InternalErrorMessage)
          } recover {
            case t: Throwable =>
              log.warn(s"Error during updateUser: ${t.getMessage}")
              InternalServerError(InternalErrorMessage)
          }
        }

      case _ =>
        Future.successful(BadRequest("Cannot parse request body: should contain first_name, last_name, or password."))
    }
  }}

  def getUserOrganizations = MeteredAction(ServiceName, "get-user-organizations") { Action.async { request =>
    withUser(request.headers) { user =>
      dataManager.getOrganizationsForUser(user) map { list =>
        Ok(Json.toJson(list))
      } recover {
        case t: Throwable =>
          log.warn(s"Error during getOrganizationsForUser: ${t.getMessage}")
          InternalServerError(InternalErrorMessage)
      }
    }
  }}

  def getUserTeams(org: String) = MeteredAction(ServiceName, "get-user-teams") { Action.async { request =>
    withUser(request.headers) { user =>
      withOrganization(org) { organization =>
        dataManager.getTeamsForUser(organization, user) map { list =>
          Ok(Json.toJson(list))
        } recover {
          case t: Throwable =>
            log.warn(s"Error during getTeamsForUser: ${t.getMessage}")
            InternalServerError(InternalErrorMessage)
        }
      }
    }
  }}

  def searchUsers(q: String) = MeteredAction(ServiceName, "search-users") { Action.async { request =>
    withUser(request.headers) { _ =>
      dataManager.findUser(q) map { list =>
        Ok(Json.toJson(list))
      } recover {
        case t: Throwable =>
          log.warn(s"Error during findUser: ${t.getMessage}")
          InternalServerError(InternalErrorMessage)
      }
    }
  }}

  private[this] def createToken(email: String, isSignUp: Boolean): ConfirmationToken = {
    val uuid = UUID.randomUUID().toString
    val creationTime = new DateTime()
    val expirationTime = creationTime.plusMinutes(60)
    ConfirmationToken(None, uuid, email, creationTime, expirationTime, isSignUp)
  }

  private[this] def withEmail(json: JsValue)
                             (body: String => Future[Result]): Future[Result] =
    (json \ "email").validate[String] match {
      case string: JsSuccess[String] => body(string.get)
      case error: JsError => Future.successful(BadRequest("Cannot parse request body: email is missing."))
    }

  private[this] def withLoginRequest(json: JsValue)(body: (String, String) => Future[Result]): Future[Result] =
    ((json \ "email").validate[String], (json \ "password").validate[String]) match {
      case (JsSuccess(email, _), JsSuccess(password, _)) => body(email, password)
      case _ => Future.successful(BadRequest("Cannot parse request body: must have an email and a password."))
    }

  private[this] def withPasswordResetRequest(json: JsValue)(body: (String, String) => Future[Result]): Future[Result] =
    ((json \ "token").validate[String], (json \ "password").validate[String]) match {
      case (JsSuccess(token, _), JsSuccess(password, _)) => body(token, password)
      case _ => Future.successful(BadRequest("Cannot parse request body: must have a token and a password."))
    }
}

object Users extends Controller with Users