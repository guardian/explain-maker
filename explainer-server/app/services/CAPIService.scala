package services

import javax.inject.Inject

import com.gu.contentapi.client.{ContentApiClientLogic, GuardianContentClient}
import com.gu.contentapi.client.model.{ItemQuery, SearchQuery, TagsQuery}
import com.gu.contentapi.client.model.v1.{ItemResponse, SearchResponse, Tag}
import com.gu.contentapi.client.Parameters
import config.Config
import play.api.cache._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CustomUrlGuardianContentClient(val apiKey: String, val apiUrl: String, val user: Option[String] = None, val pass: Option[String] = None) extends ContentApiClientLogic {
  val useThrift = false
  val urlWithAuth = for {
    u <- user
    p <- pass
  } yield s"https://$u:$p@$apiUrl"

  override val targetUrl = urlWithAuth.getOrElse(apiUrl)
}

class CAPIService @Inject() (config: Config, cache: CacheApi) {

  val client = new CustomUrlGuardianContentClient(config.capiKey, config.capiUrl)
  val previewClient = new CustomUrlGuardianContentClient(config.capiKey, config.capiPreviewUrl, config.capiPreviewUsername, config.capiPreviewPassword)

  import scala.concurrent.ExecutionContext.Implicits.global

  private def getTrackingTagsFromCapi: Future[Seq[Tag]] = {

    val trackingTagsQuery = TagsQuery().tagType("tracking").pageSize(200)
    client.getResponse(trackingTagsQuery).map(r => {
      cache.set("trackingTags", r.results, 10 minutes)
      r.results
    })

  }

  def findExplainerUsages(explainerId: String): Future[Seq[String]] = {
    val searchQuery = SearchQuery().q(explainerId)
    client.getResponse(searchQuery).map(_.results.map(_.webUrl))
  }

  def getTrackingTags: Future[Seq[Tag]] = {
    cache.get[Seq[Tag]]("trackingTags").fold(getTrackingTagsFromCapi)(Future(_))
  }

  def checkExplainerInCapi(id: String): Future[Boolean] = {
    val itemQuery = ItemQuery(s"/atom/explainer/$id")

    client.getResponse(itemQuery).map(_ => true) recover {
      case _ => false
    }
  }

}