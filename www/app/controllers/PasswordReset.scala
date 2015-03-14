package controllers

import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class PasswordReset extends AbstractCaveController {

  val startResetPasswordForm = Form(
    "email" -> email
  )

  val confirmResetPasswordForm = Form(
    tuple(
      "password1" -> text,
      "password2" -> text
    ) verifying(Messages("cave.login.signup.passwordDoesntMatch"), fields => fields match {
      case (password1: String, password2: String) => password1 == password2
    })
  )

  def startResetPassword = Action { implicit request =>
    Ok(views.html.loginscreen.startResetPassword(startResetPasswordForm))
  }

  def handleStartResetPassword = Action.async { implicit request =>
    withCaveClient { client =>
      startResetPasswordForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.html.loginscreen.startResetPassword(formWithErrors))),
        email => {
          client.Users.postForgotPassword(email) map {
            _ => Redirect(routes.Authentication.login).flashing("success" -> Messages("cave.login.passwordReset.checkYourEmail", email))
          } recover {
            case NonFatal(e) =>
              Logger.error(s"Password reset error for $email", e)
              Redirect(routes.Authentication.login).flashing("error" -> Messages("cave.login.passwordReset.error"))
          }
        }
      )
    }
  }

  def resetPassword(mailToken: String) = Action { implicit request =>
    Ok(views.html.loginscreen.resetPasswordPage(confirmResetPasswordForm, mailToken))
  }

  def handleResetPassword(mailToken: String) = Action.async { implicit request =>
    withCaveClient { client =>
      confirmResetPasswordForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.html.loginscreen.resetPasswordPage(formWithErrors, mailToken))),
        values => {
          client.Users.postResetPassword(values._1, mailToken) map {
            _ => debug(s"Password changed")
              Redirect(routes.Authentication.login).flashing("success" -> Messages("cave.login.password.reset.success"))
          } recover {
            case NonFatal(e) =>
              Logger.error("Password change error", e)
              Redirect(routes.Authentication.login).flashing("error" -> Messages("cave.login.passwordReset.error"))
          }
        }
      )
    }
  }
}