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
import models.{ExplainerStore, PublishResult, Disabled => PublishDisabled, Fail => PublishFail, Success => PublishSuccess}
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.Controller
import services.{PublicSettingsService, CAPIService}
import shared.ExplainerApi
import shared.models.CsAtom
import upickle.Js
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.gu.pandomainauth.model.{User => PandaUser}
import play.api.cache.CacheApi
import shared.models.PublicationStatus._
import util.HelperFunctions

object AutowireServer extends autowire.Server[Js.Value, Reader, Writer]{
  def read[Result: Reader](p: Js.Value) = upickle.default.readJs[Result](p)
  def write[Result: Writer](r: Result) = upickle.default.writeJs(r)
}

class ExplainerApiImpl(
  config: Config,
  previewAtomPublisher: PreviewAtomPublisher,
  liveAtomPublisher: LiveAtomPublisher,
  val publicSettingsService: PublicSettingsService,
  user: PandaUser,
  cache: CacheApi) extends ExplainerApi {

  val explainerDB = new ExplainerDB(config)
  val explainerStore = new ExplainerStore(config)
  val capiService = new CAPIService(config, cache)

  def sendKinesisEvent(explainer: Atom, actionMessage: String, atomPublisher: AtomPublisher, eventType: EventType = EventType.Update): PublishResult = {
    if (config.publishToKinesis) {
      val event = ContentAtomEvent(explainer, eventType, DateTime.now.getMillis)
      atomPublisher.publishAtomEvent(event) match {
        case Success(_) => {
          Logger.info(s"$actionMessage succeeded")
          PublishSuccess
        }
        case Failure(err) => {
          Logger.error(s"$actionMessage failed", err)
          PublishFail
        }
      }
    }
    else {
      Logger.info(s"Not $actionMessage - kinesis publishing disabled in config")
      PublishDisabled
    }
  }

  override def update(id: String, fieldName: String, value: String): Future[CsAtom] = {
    explainerStore.update(id, Symbol(fieldName), value, user).map( e => {
      sendKinesisEvent(e, s"Publishing update for explainer ${e.id} to PREVIEW kinesis", previewAtomPublisher)
      CsAtom.atomToCsAtom(e)
    })
  }

  override def load(id: String): Future[CsAtom] = explainerDB.load(id).map(CsAtom.atomToCsAtom)

  override def create(): Future[CsAtom] = {
    explainerStore.create(user).map(e => {
      sendKinesisEvent(e, s"Publishing new explainer ${e.id} to PREVIEW kinesis", previewAtomPublisher)
      CsAtom.atomToCsAtom(e)
    })
  }

  override def publish(id: String): Future[CsAtom] = {
    explainerStore.publish(id, user).map(e => {
      sendKinesisEvent(e, s"Publishing explainer ${e.id} to LIVE kinesis", liveAtomPublisher)
      CsAtom.atomToCsAtom(e)
    })
  }

  override def takeDown(id: String): Future[CsAtom] = {
    explainerStore.takeDown(id, user).map(explainerToTakeDown => {
      sendKinesisEvent(explainerToTakeDown, s"Sending takedown event for explainer ${explainerToTakeDown.id} to LIVE kinesis.", liveAtomPublisher, EventType.Takedown)
      CsAtom.atomToCsAtom(explainerToTakeDown)
    })
  }

  override def getStatus(id:String, checkCapiStatus: Boolean): Future[PublicationStatus] = {
    for {
      e <- explainerDB.load(id)
      s <- HelperFunctions.getExplainerStatus(e, capiService, checkCapiStatus, config.stage)
    } yield s
  }

}

class ApiController @Inject() (val config: Config,
  val previewAtomPublisher: PreviewAtomPublisher,
  val publicSettingsService: PublicSettingsService,
  val liveAtomPublisher: LiveAtomPublisher,
  cache: CacheApi) extends Controller with AuthActions {

  val pandaAuthenticated = new PandaAuthenticated(config)

  def autowireApi(path: String) = pandaAuthenticated.async(parse.json) { implicit request =>
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
