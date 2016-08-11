package services

import fr.hmil.roshttp.HttpRequest
import models.Tag
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.{global => g}



object CAPIService {
  def capiTagRequest(parameters: Seq[(String, String)]): Future[List[Tag]] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val parametersWithApiKey = ("api-key", g.CONFIG.CAPI_API_KEY.toString) +: parameters
    println(parametersWithApiKey)
    val request = HttpRequest("https://content.guardianapis.com/tags")
      .withQueryParameters(parametersWithApiKey:_*)

    request.send().map(response => read[List[Tag]](JSON.stringify(JSON.parse(response.body).response.results)))
  }
}