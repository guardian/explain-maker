package controllers

import javax.inject.Inject

import actions.ActionRefiners.PandaAuthenticated
import autowire.Core.Request
import models.ExplainerStore
import play.api.mvc.Controller
import shared.{Explainer, ExplainerApi}
import upickle.Js
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AutowireServer extends autowire.Server[Js.Value, Reader, Writer]{
  def read[Result: Reader](p: Js.Value) = upickle.default.readJs[Result](p)
  def write[Result: Writer](r: Result) = upickle.default.writeJs(r)
}

class ApiController @Inject() () extends Controller with ExplainerApi  {

  def autowireApi(path: String) = PandaAuthenticated.async(parse.json) { implicit request =>
    val autowireRequest: Request[Js.Value] = autowire.Core.Request(
      path.split("/"),
      upickle.json.read(request.body.toString()).asInstanceOf[Js.Obj].value.toMap
    )

    AutowireServer.route[ExplainerApi](this)(autowireRequest).map(responseJS => {
      Ok(upickle.json.write(responseJS))
    })
  }

  override def update(id: String, fieldName: String, value: String): Future[Explainer] =
    ExplainerStore.update(id, Symbol(fieldName), value)

  override def load(id: String): Future[Explainer] = ExplainerStore.load(id)

}
