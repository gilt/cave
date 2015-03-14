package controllers

import com.cave.metrics.data._
import init.Init
import org.apache.commons.codec.net.URLCodec
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.JsValue
import play.api.mvc._
import play.mvc.Http

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait AbstractApiController {
  this: Controller =>

  lazy val ServiceName = "api"

  lazy val baseUrl = Init.baseUrl
  lazy val dataManager: DataManager = Init.awsWrapper.dataManager
  lazy val awsWrapper  = Init.awsWrapper
  lazy val maxTokens  = Init.maxTokens
  implicit lazy val dataSink: DataSink = Init.awsWrapper.dataSink
  lazy val influxClientFactory = Init.influxClientFactory
  lazy val alertManager = Init.alertManager
  lazy val mailService = Init.mailService
  lazy val passwordHelper = Init.passwordHelper
  implicit val metricsEnabled: Boolean = true
  implicit lazy val metricRegistry = Init.metricRegistry

  final val InternalErrorMessage = "Oops! We've experienced an internal error. Please try your request again later."

  protected def makeLocation(org: Organization): String = {
    s"$baseUrl/organizations/${org.name}"
  }

  protected def makeLocation(org: Organization, team: Team): String = {
    s"$baseUrl/organizations/${org.name}/teams/${team.name}"
  }

  protected def makeLocation(org: Organization, token: Token): String = {
    s"$baseUrl/organizations/${org.name}/tokens/${token.id.getOrElse("")}"
  }

  protected def makeLocation(org: Organization, team: Team, token: Token): String = {
    s"$baseUrl/organizations/${org.name}/teams/${team.name}/tokens/${token.id.getOrElse("")}"
  }

  protected def makeLocation(org: Organization, alert: Alert): String = {
    s"$baseUrl/organizations/${org.name}/alerts/${alert.id.getOrElse("")}"
  }

  protected def makeLocation(org: Organization, team: Team, alert: Alert): String = {
    s"$baseUrl/organizations/${org.name}/teams/${team.name}/alerts/${alert.id.getOrElse("")}"
  }

  def withOrganization(name: String)
                      (block: Organization => Future[Result]): Future[Result] =
    dataManager.getOrganization(name) match {
      case Success(Some(org)) => block(org)
      case Success(None) => Future.successful(NotFound)
      case Failure(e) =>
        println(s"Error during getOrganization: ${e.getMessage}")
        Future.successful(InternalServerError(InternalErrorMessage))
    }

  def withTeam(organization: Organization, name: String)(block: Team => Future[Result]): Future[Result] =
    dataManager.getTeam(organization, name) match {
      case Success(Some(team)) => block(team)
      case Success(None) => Future.successful(NotFound)
      case Failure(e) =>
        println(s"Error during getTeam: ${e.getMessage}")
        Future.successful(InternalServerError(InternalErrorMessage))
    }

  def withOrganizationRole(user: User, org: Organization, roles: List[Role])
                          (block: => Future[Result])
                          (implicit ec: ExecutionContext): Future[Result] = {
    dataManager.getOrganizationsForUser(user) flatMap { list =>
      if (hasRoleForName(list, org.name, roles)) block
      else Future.successful(Forbidden)
    } recover {
      case e: Exception =>
        println(s"Error during getOrganizationsForUser: ${e.getMessage}")
        InternalServerError(InternalErrorMessage)
    }
  }

  def withTeamRole(user: User, org: Organization, team: Team, roles: List[Role])
                  (block: => Future[Result])
                  (implicit ec: ExecutionContext): Future[Result] =
    dataManager.getTeamsForUser(org, user) flatMap { list =>
      if (hasRoleForName(list, team.name, roles)) block
      else withOrganizationRole(user, org, Role.AdminsOnly)(block)
    }

  private[this] def hasRoleForName(list: List[(String, Role)], name: String, roles: List[Role]): Boolean =
    list.exists { case (entity, role) => entity.equals(name) && roles.contains(role)}

  val BasicAuth = WWW_AUTHENTICATE -> """Basic realm="Restricted""""

  private[this] def getToken(headers: Headers): Option[String] =
    headers.get(Http.HeaderNames.AUTHORIZATION) flatMap { received =>
      if (received.startsWith("Bearer")) Some(received.replaceFirst("Bearer ", ""))
      else if (received.startsWith("Basic")) {
        Some(new String((new sun.misc.BASE64Decoder).decodeBuffer(received.replaceFirst("Basic ", "")), "UTF-8").replaceFirst(":", ""))
      } else None
    }


  def withUser(headers: Headers)(block: User => Future[Result])
              (implicit ec: ExecutionContext): Future[Result] = getToken(headers) match {
    case Some(value) =>
      val exp = now()
      dataManager.findUserByToken(value, now()) flatMap {
        case Some(user) => block(user)
        case None => Future.successful(Forbidden)
      } recover {
        case NonFatal(e) => InternalServerError(InternalErrorMessage)
      }

    case None => Future.successful(Unauthorized.withHeaders(BasicAuth))
  }

  def withNewUser(json: JsValue)(block: (String, Role) => Future[Result]): Future[Result] =
    ((json \ "email").asOpt[String], (json \ "role").asOpt[Role]) match {
      case (Some(email), Some(role)) if role.isValid => block(email, role)
      case _ => Future.successful(BadRequest("Unable to parse request body: must have both 'email' and 'role'."))
    }

  def withRole(json: JsValue)(block: Role => Future[Result]): Future[Result] =
    (json \ "role").asOpt[Role] match {
      case Some(role) => block(role)
      case None => Future.successful(BadRequest("Cannot parse request body: 'role' is missing."))
    }

  def checkAuthorization(headers: Headers, tokens: Seq[Token])
                        (block: => Future[Result]): Future[Result] = getToken(headers) match {
    case Some(value) =>
      if (tokens.exists(_.value == value)) block
      else Future.successful(Forbidden)
    case None => Future.successful(Unauthorized.withHeaders(BasicAuth))
  }

  def codec = new URLCodec()
  def isValid(name: String): Boolean = codec.encode(name).equals(name)

  /**
   * Extracted as method so we can override in tests
   *
   * @return  the current date time
   */
  private[controllers] def now() = new DateTime(DateTimeZone.UTC)
}
