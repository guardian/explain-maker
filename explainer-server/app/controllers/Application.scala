package controllers


import play.api.mvc._
import shared.SharedMessages
import javax.inject._

import actions.AuthActions
import config.Config
import services.PublicSettingsService

class Application @Inject()(val publicSettingsService: PublicSettingsService, val config: Config) extends Controller with AuthActions {

  val pandaAuthenticated = new PandaAuthenticated((config))

  def index = pandaAuthenticated { implicit request =>
    Ok(views.html.index(SharedMessages.itWorks))
  }

}
