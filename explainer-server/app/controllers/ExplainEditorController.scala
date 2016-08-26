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
import shared.models.CsAtom
import shared.util.ExplainerAtomImplicits
import services.CAPIService

import scala.util.{Failure, Success}


object DeskStringUtilities {
  def deskToQueryStringWithTrailingAmpersand(desk: Option[String]): String = {
    desk match {
      case Some(tag) => s"desk=${tag}&"
      case None => ""
    }
  }
}

object Paginator {
  val pageSize: Int = 12
  def previousFragment(desk:Option[String], pageNumber: Int): String = {
    if(pageNumber>1){
      " | <a href=\"/?" +DeskStringUtilities.deskToQueryStringWithTrailingAmpersand(desk)+ "pageNumber=" + (pageNumber-1).toString() + "\">previous</a>"
    }else{
      ""
    }
  }
  def nextFragment(desk:Option[String], pageNumber: Int, maxPageNumber: Int): String = {
    if(pageNumber<maxPageNumber){
      " | <a href=\"?" +DeskStringUtilities.deskToQueryStringWithTrailingAmpersand(desk)+ "pageNumber=" + (pageNumber+1).toString() + "\">next</a>"
    }else{
      ""
    }
  }
  def maxPageNumber(numberOfExplainers: Int): Int = {
    (numberOfExplainers.toFloat/pageSize).toInt+1
  }
}

class ExplainEditorController @Inject() (val publicSettingsService: PublicSettingsService, config: Config, cache: CacheApi) extends Controller with AuthActions with ExplainerAtomImplicits {

  val pandaAuthenticated = new PandaAuthenticated(config)
  val explainerDB = new ExplainerDB(config)
  val capiService = new CAPIService(config, cache)

  def get(id: String) = pandaAuthenticated.async { implicit request =>
    val viewConfig = Json.obj(
      "CAPI_API_KEY" -> config.capiKey,
      "INTERACTIVE_URL" -> config.interactiveUrl,
      "PRESENCE_ENDPOINT_URL" -> config.presenceEndpointURL
    )
    explainerDB.load(id).map(e => {
      Ok(views.html.explainEditor(e,request.user,viewConfig))
    })
  }

  def listExplainers(desk: Option[String], maybePageNumber: Option[Int]) = pandaAuthenticated.async{ implicit request =>

    val pageNumber: Int = maybePageNumber.getOrElse(1)

    def sorting(e1: Atom, e2: Atom): Boolean = {
      val time1:Long = e1.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
      val time2:Long = e2.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
      time1 > time2
    }

    def selectPageExplainers(explainers: Seq[Atom], pageNumber: Int, pageSize: Int) = {
      // Here we drop pageNumber*pageSize and then keep the next pageSize elements.
      explainers.drop((pageNumber-1)*pageSize).take(pageSize)
    }

    val result = for {
      explainers <- explainerDB.all
      trackingTags <- capiService.getTrackingTags
    } yield {

      val trackingTagsWithAssociatedExplainers = trackingTags.filter(t => explainers.flatMap(_.tdata.tags.getOrElse(Seq())).distinct.contains(t.id))

      val explainersForDesk = desk.fold(explainers)(d => explainers.filter(_.tdata.tags.exists(_.contains(d))))
      val explainersWithSorting = explainersForDesk.sortWith(sorting)
      val explainersForPage = selectPageExplainers(explainersWithSorting,pageNumber,Paginator.pageSize)

      // Pagination

      val maxPageNumber: Int = Paginator.maxPageNumber(explainersWithSorting.length)
      val previousFragmentHTML: String = Paginator.previousFragment(desk,pageNumber)
      val nextFragmentHTML: String = Paginator.nextFragment(desk,pageNumber,maxPageNumber)

      Ok(views.html.explainList(explainersForPage, request.user, trackingTagsWithAssociatedExplainers, desk, pageNumber, previousFragmentHTML, nextFragmentHTML))

    }
    result.recover{ case err =>
      Logger.error("Error fetching explainers from dynamo", err)
      InternalServerError(err.getMessage)
    }
  }
}
