package controllers

import javax.inject.Inject

import actions.AuthActions
import autowire.Core.Request
import com.gu.contentatom.thrift.Atom
import contentatom.explainer.ExplainerAtom
import db.ExplainerDB
import models.ExplainerStore
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Controller
import services.PublicSettingsService
import shared._
import shared.models.CsAtom
import upickle.Js
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AutowireServer extends autowire.Server[Js.Value, Reader, Writer]{
  def read[Result: Reader](p: Js.Value) = upickle.default.readJs[Result](p)
  def write[Result: Writer](r: Result) = upickle.default.writeJs(r)
}

class ApiController @Inject() (val publicSettingsService: PublicSettingsService) extends Controller with ExplainerApi with AuthActions  {

  def autowireApi(path: String) = PandaAuthenticated.async(parse.json) { implicit request =>
    val autowireRequest: Request[Js.Value] = autowire.Core.Request(
      path.split("/"),
      upickle.json.read(request.body.toString()).asInstanceOf[Js.Obj].value.toMap
    )

    AutowireServer.route[ExplainerApi](this)(autowireRequest).map(responseJS => {
      Ok(upickle.json.write(responseJS))
    })
  }

  override def update(id: String, fieldName: String, value: String): Future[CsAtom] = {
    ExplainerStore.update(id, Symbol(fieldName), value).map(CsAtom.atomToCsAtom)
  }

  override def load(id: String): Future[CsAtom] = ExplainerDB.load(id).map(CsAtom.atomToCsAtom)

  override def create(): Future[CsAtom] = ExplainerStore.create().map(CsAtom.atomToCsAtom)

//  override def publish(id: String): Future[CsAtom] = {
////    load(id).map( explainer => ExplainerStore.store(
////      ExplainerItem(
////        explainer.id,
////        explainer.draft,
////        Some(
////          ExplainerAtom(explainer.draft.title,explainer.draft.body,explainer.draft.displayType)
////        )
////      )
////    ))
//    load(id).map(CsAtom.atomToCsAtom)
//  }

}
