package controllers

import play.api.libs.json.Json
import play.api.mvc._

trait Internal extends Controller {
  this: Controller =>

  final val Healthy = "healthy"
  final val Unhealthy = "unhealthy"

  def healthCheck = Action { request =>
    Ok(Healthy)
  }

  def version = Action { request =>
    Ok(Json.toJson(Map("name" -> getClass.getPackage.getImplementationTitle, "version" -> getClass.getPackage.getImplementationVersion)))
  }
}

object Internal extends Controller with Internal