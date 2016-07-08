package controllers

import actions.ActionRefiners.PandaAuthenticated
import play.api.mvc._
import shared.SharedMessages

class Application extends Controller {

  def index = PandaAuthenticated { implicit request =>
    Ok(views.html.index(SharedMessages.itWorks))
  }

}
