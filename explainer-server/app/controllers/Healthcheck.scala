package controllers

import play.api.mvc._

object Healthcheck extends Controller {

  def healthcheck = Action {
    Ok
  }

}