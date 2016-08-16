package controllers

import javax.inject.Inject

import actions.AuthActions
import com.gu.contentatom.thrift.Atom
import com.gu.scanamo.syntax.{set => _}
import config.Config
import db.ExplainerDB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.Logger
import play.api.libs.json.Json
import services.PublicSettingsService
import shared._
import shared.models.CsAtom
import shared.util.ExplainerAtomImplicits

class ExplainEditorController @Inject() (val publicSettingsService: PublicSettingsService, config: Config) extends Controller with AuthActions with ExplainerAtomImplicits {

  val pandaAuthenticated = new PandaAuthenticated(config)
  val explainerDB = new ExplainerDB(config)

  def get(id: String) = pandaAuthenticated.async { implicit request =>
    val viewConfig = Json.obj(
      "CAPI_API_KEY" -> config.capiKey
    )

    explainerDB.load(id).map(e => {
      Ok(views.html.explainEditor(e,request.user,viewConfig))
    })
  }

  def all = pandaAuthenticated.async{ implicit request =>

    explainerDB.all.map{ r =>
        def sorting(e1: Atom, e2: Atom): Boolean = {
          val time1:Long = e1.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
          val time2:Long = e2.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
          time1 > time2
        }
        Ok(views.html.explainList(r.sortWith(sorting),request.user))
    }.recover{ case err =>
      Logger.error("Error fetching explainers from dynamo", err)
      InternalServerError(err.getMessage)
    }
  }
}
