package controllers

import controllers.helpers.CaveForms._
import controllers.helpers.{DeleteToken, CreateToken}
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Tokens extends AbstractCaveController {
  def create = caveAsyncAction { implicit request =>
    createTokenForm.bindFromRequest().fold(handleFormError, handleCreateToken)
  }

  def delete = caveAsyncAction { implicit request =>
    deleteTokenForm.bindFromRequest().fold(handleFormError, handleDeleteToken)
  }

  private def handleDeleteToken(token: DeleteToken)(implicit request: Request[AnyContent]): Future[Result] = {
    token.teamName match {
      case Some(team) => deleteTeamToken(token, team)
      case _ => deleteOrganizationToken(token)
    }
  }

  private def handleCreateToken(token: CreateToken)(implicit request: Request[AnyContent]): Future[Result] = {
    token.teamName match {
      case Some(team) => createTeamToken(token, team)
      case _ => createOrganizationToken(token)
    }
  }

  private def deleteOrganizationToken(token: DeleteToken)
                                     (implicit request: Request[AnyContent]): Future[Result] = withCaveClient { client =>
    client.Tokens.deleteOrganizationsByOrganizationAndId(token.orgName, token.tokenId) map { result =>
      Logger.debug(s"Deleted organization token $token")
      Redirect(routes.Organizations.organization(token.orgName)).flashing("success" -> Messages("cave.tokens.delete.success"))
    }
  }

  private def createOrganizationToken(token: CreateToken)
                                     (implicit request: Request[AnyContent]): Future[Result] = withCaveClient { client =>
    client.Tokens.postOrganizationsByOrganization(token.orgName, token.description) map { result =>
      Logger.debug(s"Created new organization token $token")
      Redirect(routes.Organizations.organization(token.orgName)).flashing("success" -> Messages("cave.tokens.created.success"))
    }
  }

  private def deleteTeamToken(token: DeleteToken, team: String)
                             (implicit request: Request[AnyContent]): Future[Result] = withCaveClient { client =>
    client.Tokens.deleteOrganizationsAndTeamsByOrganizationAndTeamAndId(token.orgName, team, token.tokenId) map { result =>
      Logger.debug(s"Deleted team token $token")
      Redirect(routes.Teams.team(token.orgName, token.teamName.getOrElse(""))).flashing("success" -> Messages("cave.tokens.delete.success"))
    }
  }

  private def createTeamToken(token: CreateToken, team: String)
                             (implicit request: Request[AnyContent]): Future[Result] = withCaveClient { client =>
    client.Tokens.postOrganizationsAndTeamsByOrganizationAndTeam(token.orgName, team, token.description) map { result =>
      Logger.debug(s"Created new team token $token")
      Redirect(routes.Teams.team(token.orgName, token.teamName.getOrElse(""))).flashing("success" -> Messages("cave.tokens.created.success"))
    }
  }
}
