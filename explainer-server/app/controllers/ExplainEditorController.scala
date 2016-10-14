package controllers

import javax.inject.Inject

import actions.AuthActions
import com.gu.atom.data.{PreviewDataStore, PublishedDataStore}
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
import util.{HelperFunctions, PaginationConfig, Paginator}
import services.CAPIService

class ExplainEditorController @Inject() (
                                          val publicSettingsService: PublicSettingsService,
                                          val previewDynamoDataStore: PreviewDataStore,
                                          val liveDynamoDataStore: PublishedDataStore,
                                          config: Config,
                                          cache: CacheApi
                                        )
  extends Controller with AuthActions with ExplainerAtomImplicits {

  val pandaAuthenticated = new PandaAuthenticated(config)
  val explainerDB = new ExplainerDB(config, previewDynamoDataStore, liveDynamoDataStore)
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
  
  def listExplainers(desk: Option[String], pageNumber: Option[Int], titleQuery: Option[String]) = pandaAuthenticated.async{ implicit request =>

    def sorting(e1: Atom, e2: Atom): Boolean = {
      val time1:Long = e1.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
      val time2:Long = e2.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
      time1 > time2
    }

    val explainListPage = explainerDB.all.map{ explainers =>

      val explainersForDesk = desk.fold(explainers)(d => explainers.filter(_.tdata.tags.exists(_.contains(d))))
      val explainersForTitleQuery = titleQuery.fold(explainersForDesk)(q => explainersForDesk.filter(_.tdata.title.toUpperCase.contains(q.toUpperCase)))
      val explainersWithSorting = explainersForTitleQuery.sortWith(sorting)
      val explainersForPage = Paginator.selectPageExplainers(explainersWithSorting, pageNumber.getOrElse(1), config.ExplainListPageSize)

      val paginationConfig = PaginationConfig(pageNumber.getOrElse(1),
        Paginator.maxPageNumber(explainersWithSorting.length, config.ExplainListPageSize))
      val workflowData = if (explainersForPage.nonEmpty) {
        explainerDB.getWorkflowData(explainersForPage.map(_.id).toList)
      } else List()
      val wfStatusMap = workflowData.map(d => (d.id, d.status)).toMap
      val publicationStatusMap = explainersForPage.map(e =>
        (e.id, HelperFunctions.getExplainerStatus(e, explainerDB))).toMap

      Ok(views.html.explainList(explainersForPage, request.user.user, desk, paginationConfig,
          wfStatusMap, publicationStatusMap, config))
    }

    explainListPage.recover{ case err =>
      Logger.error("Error fetching explainers from dynamo", err)
      InternalServerError(err.getMessage)
    }

  }

  def findUsages(id: String, title:Option[String]) = pandaAuthenticated.async { implicit request =>
    capiService.findExplainerUsages(s""""$id"""").map{ usages =>
      Ok(views.html.usages(title.getOrElse(id), usages))
    }
  }
}
