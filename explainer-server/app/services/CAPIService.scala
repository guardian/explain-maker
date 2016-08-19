package services

import javax.inject.Inject

import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.TagsQuery
import com.gu.contentapi.client.model.v1.Tag
import config.Config
import play.api.cache._

import scala.concurrent.Future
import scala.concurrent.duration._

class CAPIService @Inject() (config: Config, cache: CacheApi) {

  val client = new GuardianContentClient(config.capiKey)

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

}