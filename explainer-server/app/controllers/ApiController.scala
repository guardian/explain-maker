package controllers

import java.util.Date
import javax.inject.Inject

import actions.AuthActions

import scala.util.{Failure, Success}
import autowire.Core.Request
import com.gu.atom.publish.{AtomPublisher, LiveAtomPublisher, PreviewAtomPublisher}
import com.gu.contentatom.thrift.{Atom, ContentAtomEvent, EventType}
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
import com.gu.pandomainauth.model.{User => PandaUser}

object AutowireServer extends autowire.Server[Js.Value, Reader, Writer]{
  def read[Result: Reader](p: Js.Value) = upickle.default.readJs[Result](p)
  def write[Result: Writer](r: Result) = upickle.default.writeJs(r)
}

class ExplainerApiImpl(
  config: Config,
  previewAtomPublisher: PreviewAtomPublisher,
  liveAtomPublisher: LiveAtomPublisher,
  val publicSettingsService: PublicSettingsService,
  user: PandaUser) extends ExplainerApi {

  val explainerDB = new ExplainerDB(config)
  val explainerStore = new ExplainerStore(config)

  def publishExplainerToKinesis(explainer: Atom, actionMessage: String, atomPublisher: AtomPublisher) = {
    if (config.publishToKinesis) {
      val event = ContentAtomEvent(explainer, EventType.Update, DateTime.now.getMillis)
      atomPublisher.publishAtomEvent(event) match {
        case Success(_) => Logger.info(s"$actionMessage succeeded")
        case Failure(err) => Logger.error(s"$actionMessage failed", err)
      }
    }
    else {
      Logger.info(s"Not $actionMessage - kinesis publishing disabled in config")
    }
  }

  override def update(id: String, fieldName: String, value: String): Future[CsAtom] = {
    val updatedExplainer = explainerStore.update(id, Symbol(fieldName), value, user)
    updatedExplainer.map(publishExplainerToKinesis(_, "Publishing explainer update to PREVIEW kinesis", previewAtomPublisher))
    updatedExplainer.map(CsAtom.atomToCsAtom)
  }

  override def load(id: String): Future[CsAtom] = explainerDB.load(id).map(CsAtom.atomToCsAtom)

  override def create(): Future[CsAtom] = {
    val newExplainer = explainerStore.create(user)
    newExplainer.map(publishExplainerToKinesis(_, "Publishing new explainer to PREVIEW kinesis", previewAtomPublisher))
    newExplainer.map(e => CsAtom.atomToCsAtom(e))

  }

  override def publish(id: String): Future[CsAtom] = {
    val _ = explainerStore.publish(id, user)
    val explainerToPublish = explainerDB.load(id)
    explainerToPublish.map(publishExplainerToKinesis(_, "Publishing explainer to LIVE kinesis", liveAtomPublisher))
    explainerToPublish.map(CsAtom.atomToCsAtom)
  }

}

class ApiController @Inject() (val config: Config,
  val previewAtomPublisher: PreviewAtomPublisher,
  val publicSettingsService: PublicSettingsService,
  val liveAtomPublisher: LiveAtomPublisher) extends Controller with AuthActions {

  val pandaAuthenticated = new PandaAuthenticated(config)

  def autowireApi(path: String) = pandaAuthenticated.async(parse.json) { implicit request =>
    val autowireRequest: Request[Js.Value] = autowire.Core.Request(
      path.split("/"),
      upickle.json.read(request.body.toString()).asInstanceOf[Js.Obj].value.toMap
    )
    val api = new ExplainerApiImpl(config, previewAtomPublisher, liveAtomPublisher, publicSettingsService, request.user.user)
    AutowireServer.route[ExplainerApi](api)(autowireRequest).map(responseJS => {
      Ok(upickle.json.write(responseJS))
    })
  }

}
