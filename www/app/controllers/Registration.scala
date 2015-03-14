package controllers

import play.api.Logger
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.data.Forms._

import scala.util.control.NonFatal

case class SignUpFormData(firstName: String, lastName: String, password1: String, password2: String)

class Registration extends AbstractCaveController {

  val startSignUpForm = Form(
    "email" -> email
  )

  val confirmSignUpForm = Form(
    mapping(
      "firstName" -> text,
      "lastName" -> text,
      "password1" -> text,
      "password2" -> text
    )(SignUpFormData.apply)(SignUpFormData.unapply)
      verifying(Messages("cave.login.signup.passwordDoesntMatch"), fields => fields match {
      case data: SignUpFormData => data.password1 == data.password2
    })
  )

  def startSignUp = Action { implicit request =>
    Ok(views.html.loginscreen.startSignUp(startSignUpForm))
  }

  def handleStartSignUp = Action.async { implicit request =>
    withCaveClient { client =>
      startSignUpForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.html.loginscreen.startSignUp(formWithErrors))),
        email => {
          client.Users.postRegister(email) map {
            _ => Redirect(routes.Authentication.login).flashing("success" -> Messages("cave.login.signup.checkYourEmail", email))
          } recover {
            case NonFatal(e) =>
              Logger.error("New user registration error", e)
              Redirect(routes.Authentication.login).flashing("error" -> Messages("cave.login.signup.error"))
          }
        }
      )
    }
  }

  def signUp(mailToken: String) = Action { implicit request =>
    Ok(views.html.loginscreen.signUp(confirmSignUpForm, mailToken))
  }

  def handleSignUp(mailToken: String) = Action.async { implicit request =>
    withCaveClient { client =>
      confirmSignUpForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.html.loginscreen.signUp(formWithErrors, mailToken))),
        formData => {
          client.Users.postConfirm(formData.firstName, formData.lastName, formData.password1, mailToken) map {
            _ => Redirect(routes.Authentication.login).flashing("success" -> Messages("cave.login.signup.youCanLoginNow", email))
          } recover {
            case NonFatal(e) =>
              Logger.error("Registration confirmation error", e)
              Redirect(routes.Authentication.login).flashing("error" -> Messages("cave.login.signup.error"))
          }
        }
      )
    }
  }
}

object Registration extends Registration