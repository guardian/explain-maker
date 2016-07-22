package controllers

import java.util.Date
import javax.inject.Inject

import actions.AuthActions

import scala.util.{Failure, Success}
import autowire.Core.Request
import com.gu.atom.publish.{LiveAtomPublisher, PreviewAtomPublisher}
import com.gu.contentatom.thrift.{ContentAtomEvent, EventType}
import config.Config
import db.ExplainerDB
import models.ExplainerStore
import org.joda.time.DateTime
import play.api.Logger
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

class ApiController @Inject() (config: Config, previewAtomPublisher: PreviewAtomPublisher) (
  val publicSettingsService: PublicSettingsService,
  val livePublisher: LiveAtomPublisher, val previewPublisher: PreviewAtomPublisher) extends Controller with ExplainerApi with AuthActions  {

  val explainerDB = new ExplainerDB(config)
  val explainerStore = new ExplainerStore(config)

  val pandaAuthenticated = new PandaAuthenticated(config)

  def autowireApi(path: String) = pandaAuthenticated.async(parse.json) { implicit request =>
    val autowireRequest: Request[Js.Value] = autowire.Core.Request(
      path.split("/"),
      upickle.json.read(request.body.toString()).asInstanceOf[Js.Obj].value.toMap
    )

    AutowireServer.route[ExplainerApi](this)(autowireRequest).map(responseJS => {
      Ok(upickle.json.write(responseJS))
    })
  }

  override def update(id: String, fieldName: String, value: String): Future[CsAtom] = {
    explainerStore.update(id, Symbol(fieldName), value).map(CsAtom.atomToCsAtom)
  }

  override def load(id: String): Future[CsAtom] = explainerDB.load(id).map(CsAtom.atomToCsAtom)

  override def create(): Future[CsAtom] = {
    val newExplainer = explainerStore.create()
    val event = newExplainer.map(ContentAtomEvent(_, EventType.Update, DateTime.now.getMillis))
    event.map(e => previewAtomPublisher.publishAtomEvent(e) match {
      case Success(_) => Logger.info("Successfully pushed new atom to kinesis")
      case Failure(err) => Logger.error(s"Failed to push atom to kinesis")
    })

    newExplainer.map(e => CsAtom.atomToCsAtom(e))

  }

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
