package services

import javax.inject.Inject

import com.gu.contentapi.client.{ContentApiClientLogic, GuardianContentClient}
import com.gu.contentapi.client.model.{ItemQuery, TagsQuery}
import com.gu.contentapi.client.model.v1.{ItemResponse, Tag}
import config.Config
import play.api.cache._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CustomUrlGuardianContentClient(val apiKey: String, val apiUrl: String) extends ContentApiClientLogic {
  val useThrift = false
  override val targetUrl = apiUrl
}

class CAPIService @Inject() (config: Config, cache: CacheApi) {

  val client = new CustomUrlGuardianContentClient(config.capiKey, config.capiUrl)

  import scala.concurrent.ExecutionContext.Implicits.global

  private def getTrackingTagsFromCapi: Future[Seq[Tag]] = {
    val trackingTagsQuery = TagsQuery().tagType("tracking").pageSize(200)
    client.getResponse(trackingTagsQuery).map(r => {
      cache.set("trackingTags", r.results, 10 minutes)
      r.results
    })
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