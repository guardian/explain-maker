package controllers

import javax.inject.Inject

import actions.AuthActions
import com.gu.scanamo._
import com.gu.scanamo.syntax.{set => _}
import models.ExplainerStore
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.PublicSettingsService
import shared.Explainer

class ExplainEditorController @Inject() (val publicSettingsService: PublicSettingsService) extends Controller with AuthActions {

  val explainersTable = Table[Explainer]("explainers")

  def get(id: String) = PandaAuthenticated { implicit request =>
    Ok(views.html.explainEditor(id, "Explain Editor"))
  }

  def all = PandaAuthenticated.async{ implicit request =>
    ExplainerStore.all.map{ r =>
        Ok(views.html.explainList(r))
    }.recover{ case err =>
      InternalServerError(err.getMessage)
    }
  }
}
