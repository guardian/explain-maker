package controllers

import actions.PanAuthenticationSettings
import play.api.mvc._

class Healthcheck extends Controller {

  def healthcheck = Action {
    PanAuthenticationSettings.publicKey.fold {
      ServiceUnavailable("The server couldn't load the Panda key used for user-authentication")
    } { _ =>
      Ok(app.BuildInfo.gitCommitId)
    }

  }

}