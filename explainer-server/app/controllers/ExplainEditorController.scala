package controllers

import javax.inject.Inject

import actions.AuthActions
import com.gu.contentatom.thrift.Atom
import config.Config
import db.ExplainerDB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.json.Json
import services.PublicSettingsService
import shared.util.ExplainerAtomImplicits
import util.{HelperFunctions, Paginator}
import services.CAPIService

class ExplainEditorController @Inject() (val publicSettingsService: PublicSettingsService, config: Config, cache: CacheApi)
  extends Controller with AuthActions with ExplainerAtomImplicits {

  val pandaAuthenticated = new PandaAuthenticated(config)
  val explainerDB = new ExplainerDB(config)
  val capiService = new CAPIService(config, cache)

  def get(id: String) = pandaAuthenticated { implicit request =>
    val viewConfig = Json.obj(
      "CAPI_KEY" -> config.capiKey,
      "CAPI_URL" -> config.capiUrl,
      "INTERACTIVE_URL" -> config.interactiveUrl,
      "PRESENCE_ENDPOINT_URL" -> config.presenceEndpointURL,
      "PRESENCE_ENABLED" -> config.presenceEnabled,
      "EXPLAINER_IDENTIFIER" -> id,
      "USER_FIRSTNAME"     -> request.user.user.firstName,
      "USER_LASTNAME"      -> request.user.user.lastName,
      "USER_EMAIL_ADDRESS" -> request.user.user.email
    )
    Ok(views.html.explainEditor(id, request.user, viewConfig))
  }

  def listExplainers(desk: Option[String], pageNumber: Int = 1) = pandaAuthenticated.async{ implicit request =>

    def sorting(e1: Atom, e2: Atom): Boolean = {
      val time1:Long = e1.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
      val time2:Long = e2.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
      time1 > time2
    }

    val result = for {
      explainers <- explainerDB.all
      trackingTags <- capiService.getTrackingTags
    } yield {

      val trackingTagsInUse = trackingTags.filter(t => explainers.flatMap(_.tdata.tags.getOrElse(Seq())).distinct.contains(t.id))

      val explainersForDesk = desk.fold(explainers)(d => explainers.filter(_.tdata.tags.exists(_.contains(d))))
      val explainersWithSorting = explainersForDesk.sortWith(sorting)
      val explainersForPage = Paginator.selectPageExplainers(explainersWithSorting, pageNumber, config.ExplainListPageSize)

      val paginationConfig = Paginator.getPaginationConfig(pageNumber, desk, explainersWithSorting, config.ExplainListPageSize)

      val workflowData = explainerDB.getWorkflowData(explainersForPage.map(_.id).toList)
      val wfStatusMap = workflowData.map(d => (d.id, d.status)).toMap
      val publicationStatusMap = explainersForPage.map(e =>
        (e.id, HelperFunctions.getExplainerStatus(e, explainerDB))).toMap

      Ok(views.html.explainList(explainersForPage, request.user.user, trackingTagsInUse, desk, paginationConfig,
        wfStatusMap, publicationStatusMap))

    }
    result.recover{ case err =>
      Logger.error("Error fetching explainers from dynamo", err)
      InternalServerError(err.getMessage)
    }
  }
}
