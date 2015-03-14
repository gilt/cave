package controllers

import com.gilt.cavellc.errors.FailedRequest
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class PasswordChange extends AbstractCaveController {

  val passwordChange = Form(
    tuple(
      "newPassword.password1" -> text,
      "newPassword.password2" -> text
    ) verifying(Messages("cave.login.signup.passwordDoesntMatch"), fields => fields match {
      case (password1: String, password2: String) => password1 == password2
    })
  )

  def page = caveAsyncAction { implicit request =>
    Future.successful(Ok(views.html.loginscreen.passwordChange(passwordChange)))
  }

  def handlePasswordChange = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      passwordChange.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.html.loginscreen.passwordChange(formWithErrors))),
        values =>
          client.Users.patchInfo(None, None, Some(values._1)) map {
            case user => Redirect(routes.UserProfile.profile()).flashing("success" -> s"${user.firstName}, your password has been changed")
          } recover {
            case error: FailedRequest if error.responseCode == FORBIDDEN =>
              Logger.debug("Your API session token has expired. Please login again.", error)
              Redirect(routes.Authentication.login).flashing("error" -> Messages("cave.login.sessionTokenExpired"))
            case NonFatal(e) =>
              Logger.error("Unable to change password", e)
              InternalServerError(views.html.errorpages.errorPage(Messages("cave.errors.5xx.passwordChange")))
          }
      )
    }
  }
}