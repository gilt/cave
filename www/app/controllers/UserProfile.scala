package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserProfile extends AbstractCaveController {

  val changeDetails = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText
    )(UserProfileChange.apply)(UserProfileChange.unapply)
  )

  def update = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      changeDetails.bindFromRequest().fold(
        formWithErrors => Future.successful(Redirect(routes.UserProfile.profile).flashing("error" -> buildFormValidationErrorMessage(formWithErrors))),
        change =>
          client.Users.patchInfo(Some(change.firstName), Some(change.lastName), None) map {
            case user => Redirect(routes.UserProfile.profile).flashing("success" -> Messages("cave.profile.changeDetails.modal.success")).
              withSession(request.session + ("userName" -> s"${user.firstName} ${user.lastName}"))
          }
      )
    }
  }

  def profile = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      client.Users.getInfo().map {
        case Some(user) => Ok(views.html.profile.profile(user, userToken, changeDetails.fill(UserProfileChange(user.firstName, user.lastName))))
        case None => InternalServerError("Unable to get user information")
      }
    }
  }
}

case class UserProfileChange(firstName: String, lastName: String)