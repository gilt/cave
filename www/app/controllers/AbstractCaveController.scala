package controllers

import com.gilt.cavellc.errors.FailedRequest
import com.gilt.cavellc.models.{UserTeam, Role, Organization, UserOrganization}
import com.gilt.cavellc.Client
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import play.api.{Logger, Play}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait AbstractCaveController extends Controller {

  val EMPTY_STRING = ""

  def caveAsyncAction(actionBlock: Request[AnyContent] => Future[Result]): Action[AnyContent] = {
    Action.async { implicit request =>
      request.session.get("sessionToken").map { caveApiUserSessionToken =>
        actionBlock(request).recover {
          case e: FailedRequest if e.responseCode == FORBIDDEN =>
            debug(e.requestUri)
            Logger.debug(s"CAVE API SESSION EXPIRED\nUI Request: $request\nCave API request:" +
              s" ${e.requestUri}\nAPI token:" +
              s" ${if (caveApiUserSessionToken.length > 20) caveApiUserSessionToken.take(18) + "[REDACTED]"}", e)
            Redirect(routes.Authentication.login).flashing("error" -> Messages("cave.login.sessionTokenExpired"), "requestedUrl" -> request.uri)
          case e: FailedRequest =>
            Logger.error(s"CAVE API ERROR\nUI Request: $request\nCave API request:" +
              s" ${e.requestUri}\nAPI token:" +
              s" ${if (caveApiUserSessionToken.length > 20) caveApiUserSessionToken.take(18) + "[REDACTED]"}", e)
            InternalServerError(views.html.errorpages.errorPage(Messages("cave.errors.5xx.general")))
          case NonFatal(e) =>
            Logger.error(s"CAVE UI Request: $request\nUnexpected error. User API session token:" +
              s" ${if (caveApiUserSessionToken.length > 20) caveApiUserSessionToken.take(18) + "[REDACTED]"}", e)
            InternalServerError(views.html.errorpages.errorPage(Messages("cave.errors.5xx.general")))
        }
      }.getOrElse {
        Logger.debug("No valid session. Please login.")
        Future.successful(Redirect(routes.Authentication.login).flashing("error" -> Messages("cave.login.session.expired"), "requestedUrl" -> request.uri))
      }
    }
  }

  def caveAsyncActionJson(actionBlock: Request[AnyContent] => Future[Result]): Action[AnyContent] = {
    Action.async { implicit request =>
      request.session.get("sessionToken").map { caveApiUserSessionToken =>
        actionBlock(request).recover {
          case e: FailedRequest if e.responseCode == FORBIDDEN =>
            debug(e.requestUri)
            Logger.debug(s"CAVE API SESSION EXPIRED\nUI Request: $request\nCave API request:" +
              s" ${e.requestUri}\nAPI token:" +
              s" ${if (caveApiUserSessionToken.length > 20) caveApiUserSessionToken.take(18) + "[REDACTED]"}", e)
            Unauthorized(Messages("cave.login.sessionTokenExpired"))
          case e: FailedRequest =>
            Logger.error(s"CAVE API ERROR\nUI Request: $request\nCave API request:" +
              s" ${e.requestUri}\nAPI token:" +
              s" ${if (caveApiUserSessionToken.length > 20) caveApiUserSessionToken.take(18) + "[REDACTED]"}", e)
            InternalServerError(Messages("cave.errors.5xx.general"))
          case NonFatal(e) =>
            Logger.error(s"CAVE UI Request: $request\nUnexpected error. User API session token:" +
              s" ${if (caveApiUserSessionToken.length > 20) caveApiUserSessionToken.take(18) + "[REDACTED]"}", e)
            InternalServerError(Messages("cave.errors.5xx.general"))
        }
      }.getOrElse {
        Logger.debug("No valid session. Please login.")
        Future.successful(Unauthorized(Messages("cave.login.session.expired")))
      }
    }
  }


  def getTeamsInOrganization(organizationName: String, organizationRole: Role, client: Client): Future[Seq[UserTeam]] = {
    organizationRole match {
      case Role.Admin =>
        for {
          teams <- client.Teams.getOrganizationsByOrganization(organizationName)
        } yield (teams.map(team => UserTeam(team.name, Role.Admin)))
      case _ => client.Users.getOrganizationsAndTeamsByName(organizationName)
    }
  }

  def getUserTeams(myOrganizations: Seq[UserOrganization], client: Client): Future[Seq[UserTeamOrganization]] = {
    Future.sequence(myOrganizations.map {
      org => client.Users.getOrganizationsAndTeamsByName(org.name).map { userTeams =>
        userTeams.map { ut =>
          UserTeamOrganization(org.name, ut)
        }
      }
    }).map(_.flatten)
  }

  private[controllers] def getOrganization(orgName: String, client: Client, myOrganizations: Seq[UserOrganization]): Future[Option[Organization]] = {
    if (myOrganizations.find(o => o.name == orgName && !Seq(Role.Viewer, Role.Team).contains(o.role)).isDefined)
      client.Organizations.getByName(orgName)
    else Future.successful(Some(Organization(orgName, "", "", Seq.empty)))
  }

  implicit def userToken(implicit request: Request[AnyContent]): String = request.session.get("sessionToken").getOrElse("NO-VALID-TOKEN")

  def withCaveClient(block: Client => Future[Result])
              (implicit userToken: String, ec: ExecutionContext): Future[Result] = {
    block(new Client(Play.configuration.getString("cave.api").getOrElse("https://api.cavellc.io"), Some(com.gilt.cavellc.Authorization.Basic(userToken))))
  }

  def withExplicitTokenClient(userToken: String)
                             (block: Client => Future[Result])
                             (implicit ec: ExecutionContext): Future[Result] = {
    block(new Client(Play.configuration.getString("cave.api").getOrElse("https://api.cavellc.io"), Some(com.gilt.cavellc.Authorization.Basic(userToken))))
  }

  private[controllers] def handleFormError(formWithError: Form[_]) = {
    Future.successful(Redirect(routes.Application.index).flashing("error" -> buildFormValidationErrorMessage(formWithError)))
  }

  private[controllers] def buildFormValidationErrorMessage[T](form: Form[T]): String = {
    val errors = form.errors.groupBy(_.key).mapValues { errors => errors.map(e => play.api.i18n.Messages(e.message, e.args: _*))}
    views.html.forms.validationError.render(errors).body
  }

  private[controllers] def buildOrganizationUrlForBreadcrumb(organizationName: String, organizations: Seq[UserOrganization]): String = {
    if (organizations.exists(o => o.name == organizationName && o.role != Role.Team))
      routes.Organizations.organization(organizationName).toString()
    else ""
  }

  private[controllers] def getRoleInTeam(organizationName: String, teamName: String, userTeams: Seq[UserTeamOrganization], roleInOrganization: Role): Future[Role] = {
    val userTeamOrganizations = userTeams.filter(t => t.organizationName == organizationName && t.userTeam.name == teamName)

    userTeamOrganizations match {
      case Seq() => Future.successful(roleInOrganization)
      case uo: Seq[UserTeamOrganization] => Future.successful(uo.head.userTeam.role)
    }
  }

  private[controllers] def debug(msg: Any)(implicit request: Request[AnyContent]) = Logger.debug(s"${if (userToken.length > 10) "User API Token: " + userToken.take(9) + "[REDACTED]"} - $msg")
}
