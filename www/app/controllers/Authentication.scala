package controllers

import com.gilt.cavellc.errors.FailedRequest
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class Authentication extends AbstractCaveController {

  val loginForm = Form(mapping("username" -> email, "password" -> text, "landingUrl" -> text)
    (UserLogin.apply)(UserLogin.unapply)
  )

  def login = Action { implicit request =>
    val loginFormWithLandingPage = loginForm.fill(UserLogin(EMPTY_STRING, EMPTY_STRING, request.flash.get("requestedUrl").getOrElse("/")))
    Ok(views.html.loginscreen.signIn(loginFormWithLandingPage))
  }

  def logout = Action.async { implicit request =>
    Future.successful(Redirect(routes.Authentication.login()).withNewSession.flashing(
      "success" -> "You've been logged out"
    ))
  }

  def authenticate = Action.async { implicit request =>
    withCaveClient { client =>
      loginForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.html.loginscreen.signIn(formWithErrors))),
        loginForm => client.Users.postLogin(loginForm.email, loginForm.password) flatMap {
          case user: com.gilt.cavellc.models.Auth =>
            withExplicitTokenClient(user.token) { signedClient =>
              signedClient.Users.getInfo().flatMap {
                case Some(apiUser) =>
                  Logger.debug(s"Login Succeeded for ${loginForm.email}")
                  val landingUrl: String = if (loginForm.landingUrl.nonEmpty) loginForm.landingUrl else routes.Application.index().url
                  Logger.debug(s"landing url ${loginForm.landingUrl}")
                  Future.successful(Redirect(landingUrl).withSession(
                    "sessionToken" -> user.token,
                    "userName" -> s"${apiUser.firstName} ${apiUser.lastName}"
                  ))
                case _ =>
                  Logger.error(s"Unable to find user details for user: ${loginForm.email}")
                  Future.successful(InternalServerError(views.html.errorpages.errorPage(Messages("cave.login.login.internalError"))))
              }
            }
        } recover {
          case error: FailedRequest if error.responseCode == BAD_REQUEST =>
            Logger.debug(s"Incorrect username or password for user ${loginForm.email}", error)
            Redirect(routes.Authentication.login()).flashing("error" -> Messages("cave.login.login.incorrectUsernameOrPassword"))
          case NonFatal(e) =>
            Logger.error(s"Unable to authenticate user: ${loginForm.email}", e)
            InternalServerError(views.html.errorpages.errorPage(Messages("cave.login.login.internalError")))
        }
      )
    }
  }
}

object Authentication extends Authentication

case class UserLogin(email: String, password: String, landingUrl: String)