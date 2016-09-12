package controllers

import javax.inject.Inject

import actions.AuthActions
import autowire.Core.Request
import com.gu.atom.publish.{LiveAtomPublisher, PreviewAtomPublisher}
import config.Config
import play.api.cache.CacheApi
import play.api.mvc.Controller
import services.PublicSettingsService
import shared.ExplainerApi
import upickle.Js
import upickle.default._

object AutowireServer extends autowire.Server[Js.Value, Reader, Writer]{
  def read[Result: Reader](p: Js.Value) = upickle.default.readJs[Result](p)
  def write[Result: Writer](r: Result) = upickle.default.writeJs(r)
}

class ApiController @Inject() (val config: Config,
  val previewAtomPublisher: PreviewAtomPublisher,
  val publicSettingsService: PublicSettingsService,
  val liveAtomPublisher: LiveAtomPublisher,
  cache: CacheApi) extends Controller with AuthActions {

  val pandaAuthenticated = new PandaAuthenticated(config)

  def autowireApi(path: String) = pandaAuthenticated.async(parse.json) { implicit request =>
    import scala.concurrent.ExecutionContext.Implicits.global
    val autowireRequest: Request[Js.Value] = autowire.Core.Request(
      path.split("/"),
      upickle.json.read(request.body.toString()).asInstanceOf[Js.Obj].value.toMap
    )
    val api = new ExplainerApiImpl(config, previewAtomPublisher, liveAtomPublisher, publicSettingsService, request.user.user, cache)
    AutowireServer.route[ExplainerApi](api)(autowireRequest).map(responseJS => {
      Ok(upickle.json.write(responseJS))
    })
  }

}