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
import play.api.cache.CacheApi
import play.api.libs.json.Json
import services.PublicSettingsService
import shared._
import shared.util.ExplainerAtomImplicits
import services.CAPIService

import scala.util.{Failure, Success}

class ExplainEditorController @Inject() (val publicSettingsService: PublicSettingsService, config: Config, cache: CacheApi) extends Controller with AuthActions with ExplainerAtomImplicits {

  val pandaAuthenticated = new PandaAuthenticated(config)
  val explainerDB = new ExplainerDB(config)
  val capiService = new CAPIService(config, cache)

  def get(id: String) = pandaAuthenticated { implicit request =>
    val viewConfig = Json.obj(
      "CAPI_API_KEY" -> config.capiKey
    )
    Ok(views.html.explainEditor(id,request.user,viewConfig))
  }

  def listExplainers(desk: Option[String]) = pandaAuthenticated.async{ implicit request =>
    def sorting(e1: Atom, e2: Atom): Boolean = {
      val time1:Long = e1.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
      val time2:Long = e2.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
      time1 > time2
    }

    val result = for {
      explainers <- explainerDB.all
      trackingTags <- capiService.getTrackingTags
    } yield {
      val explainersForDesk = desk.fold(explainers)(d => explainers.filter(_.tdata.tags.exists(_.contains(d))))
      val trackingTagsWithAssociatedExplainer = trackingTags.filter(t => explainers.flatMap(_.tdata.tags.getOrElse(Seq())).distinct.contains(t.id))

      Ok(views.html.explainList(explainersForDesk.sortWith(sorting), request.user, trackingTagsWithAssociatedExplainer, desk))
    }
    result.recover{ case err =>
      Logger.error("Error fetching explainers from dynamo", err)
      InternalServerError(err.getMessage)
    }
  }
}
