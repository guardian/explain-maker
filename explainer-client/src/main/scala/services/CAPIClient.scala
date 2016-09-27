package services

import fr.hmil.roshttp.HttpRequest
import shared.models.CsTag
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.{global => g}



object CAPIClient {

  val apiKeyParam = ("api-key", g.CONFIG.CAPI_KEY.toString)

  // client side, we have to always query PROD capi as CODE isn't available on https
  val capiUrl = "https://content.guardianapis.com"

  def capiTagRequest(parameters: Seq[(String, String)]): Future[List[CsTag]] = {
    import scala.concurrent.ExecutionContext.Implicits.global


    val parametersWithApiKey = apiKeyParam +: parameters

    val request = HttpRequest(s"$capiUrl/tags")
      .withQueryParameters(parametersWithApiKey:_*)

    request.send().map(response => read[List[CsTag]](JSON.stringify(JSON.parse(response.body).response.results)))
  }
}