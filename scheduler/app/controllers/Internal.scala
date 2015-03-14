package controllers

import actors.Coordinator
import akka.actor.Inbox
import akka.pattern.ask
import akka.util.Timeout
import init.Init
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.duration._

object Internal extends Controller {

  def actorSystem = Init.system
  def coordinator = Init.coordinator

  def healthCheck() = Action { request =>
    // TODO: add real checks
    Ok("healthy")
  }

  def status() = Action.async { request =>
    implicit val inbox = Inbox.create(actorSystem)
    implicit val timeout = Timeout(3.seconds)
    import scala.concurrent.ExecutionContext.Implicits.global

    ask(coordinator, Coordinator.StatusRequest).mapTo[Coordinator.StatusResponse] map { response =>
      Ok(Json.toJson(response))
    }
  }
}

