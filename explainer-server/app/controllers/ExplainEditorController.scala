package controllers

import javax.inject.Inject

import actions.AuthActions
import com.gu.scanamo._
import com.gu.scanamo.syntax.{set => _}
import models.ExplainerStore
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.PublicSettingsService
import shared._

class ExplainEditorController @Inject() (val publicSettingsService: PublicSettingsService) extends Controller with AuthActions {

  val explainersTable = Table[ExplainerItem]("explainers")

  def get(id: String) = PandaAuthenticated { implicit request =>
    Ok(views.html.explainEditor(id, "Explain Editor"))
  }

  def all = PandaAuthenticated.async{ implicit request =>

    ExplainerStore.all.map{ r =>
        def sorting(e1: ExplainerItem, e2: ExplainerItem): Boolean = {
          val time1:Long = e1.draft.last_modified_time
          val time2:Long = e2.draft.last_modified_time
          time1 > time2
        }
        Ok(views.html.explainList(r.sortWith(sorting)))
    }.recover{ case err =>
      InternalServerError(err.getMessage)
    }
  }
}
