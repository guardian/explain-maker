package controllers


import play.api.mvc._
import shared.SharedMessages
import javax.inject._
import actions.AuthActions
import services.PublicSettingsService

class Application @Inject()(val publicSettingsService: PublicSettingsService) extends Controller with AuthActions {

  def index = PandaAuthenticated { implicit request =>
    Ok(views.html.index(SharedMessages.itWorks))
  }



}
