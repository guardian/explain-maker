package controllers

import javax.inject.Inject
import play.api.mvc._
import services.PublicSettingsService

class Healthcheck @Inject() (val publicSettingsService: PublicSettingsService) extends Controller {

  def healthcheck = Action {
    publicSettingsService.publicSettings.publicKey.fold {
      ServiceUnavailable("The server couldn't load the Panda key used for user-authentication")
    } { _ =>
      Ok(app.BuildInfo.gitCommitId)
    }

  }

}