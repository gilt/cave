package controllers

import play.api.mvc._

object Internal extends Controller {

  def healthCheck() = Action { request =>
    // TODO: add checks for InfluxDB
    Ok("healthy")
  }
}

