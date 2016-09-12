package controllers

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.joda.time.DateTime
import play.api.cache.CacheApi
import play.api.Logger

import com.gu.atom.publish.{AtomPublisher, LiveAtomPublisher, PreviewAtomPublisher}
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.explainer.{DisplayType => ThriftDisplayType, ExplainerAtom}
import config.Config
import db.ExplainerDB
import models.{PublishResult, Disabled => PublishDisabled, Fail => PublishFail, Success => PublishSuccess}
import services.{CAPIService, PublicSettingsService}
import shared.ExplainerApi
import shared.models.{CsAtom, ExplainerUpdate}
import shared.util.ExplainerAtomImplicits._
import com.gu.pandomainauth.model.{User => PandaUser}

import shared.models.PublicationStatus._
import util.HelperFunctions.{contentChangeDetailsBuilder, applyExplainerUpdate, getExplainerStatus}


class ExplainerApiImpl(
  config: Config,
  previewAtomPublisher: PreviewAtomPublisher,
  liveAtomPublisher: LiveAtomPublisher,
  val publicSettingsService: PublicSettingsService,
  user: PandaUser,
  cache: CacheApi) extends ExplainerApi {

  val explainerDB = new ExplainerDB(config)
  val capiService = new CAPIService(config, cache)

  override def create(): Future[CsAtom] = {
    val explainer = Atom(
      id = java.util.UUID.randomUUID.toString,
      atomType = AtomType.Explainer,
      defaultHtml = "-",
      data = AtomData.Explainer(ExplainerAtom("", "", ThriftDisplayType.Flat)),
      contentChangeDetails = contentChangeDetailsBuilder(user, None, updateCreated = true, updateLastModified = true)
    )
    explainerDB.create(explainer)
    sendKinesisEvent(explainer, s"Publishing new explainer ${explainer.id} to PREVIEW kinesis", previewAtomPublisher)
    Future(CsAtom.atomToCsAtom(explainer))
  }

  override def load(id: String): Future[CsAtom] = explainerDB.load(id).map(CsAtom.atomToCsAtom)

  override def update(id: String, update: ExplainerUpdate): Future[CsAtom] = {
    explainerDB.load(id).map( explainer => {
      val updatedExplainer = explainer.copy(
        data = applyExplainerUpdate(explainer.tdata, update),
        contentChangeDetails = contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails),updateLastModified = true)
      )
      explainerDB.update(updatedExplainer)
      sendKinesisEvent(updatedExplainer, s"Publishing update for explainer ${updatedExplainer.id} to PREVIEW kinesis", previewAtomPublisher)
      CsAtom.atomToCsAtom(updatedExplainer)
    })
  }

  override def publish(id: String): Future[CsAtom] = {
    explainerDB.load(id).map{ explainer =>
      val updatedExplainer = explainer.copy(
        contentChangeDetails=contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails), updatePublished = true)
      )
      explainerDB.update(updatedExplainer)
      sendKinesisEvent(updatedExplainer, s"Publishing explainer $updatedExplainer.id} to LIVE kinesis", liveAtomPublisher)
      CsAtom.atomToCsAtom(updatedExplainer)
    }
  }

  override def takeDown(id: String): Future[CsAtom] = {
    explainerDB.load(id).map( explainer => {
      val updatedExplainer = explainer.copy(
        contentChangeDetails=contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails), updateLastModified = true)
      )
      explainerDB.update(updatedExplainer)
      sendKinesisEvent(updatedExplainer, s"Sending takedown event for explainer ${updatedExplainer.id} to LIVE kinesis.", liveAtomPublisher, EventType.Takedown)
      CsAtom.atomToCsAtom(updatedExplainer)
    })
  }

  override def getStatus(id:String, checkCapiStatus: Boolean): Future[PublicationStatus] = {
    for {
      e <- explainerDB.load(id)
      s <- getExplainerStatus(e, capiService, checkCapiStatus, config.stage)
    } yield s
  }

  private def sendKinesisEvent(explainer: Atom, actionMessage: String, atomPublisher: AtomPublisher, eventType: EventType = EventType.Update): PublishResult = {
    if (config.publishToKinesis) {
      val event = ContentAtomEvent(explainer, eventType, DateTime.now.getMillis)
      atomPublisher.publishAtomEvent(event) match {
        case Success(_) =>
          Logger.info(s"$actionMessage succeeded")
          PublishSuccess
        case Failure(err) =>
          Logger.error(s"$actionMessage failed", err)
          PublishFail
      }
    }
    else {
      Logger.info(s"Not $actionMessage - kinesis publishing disabled in config")
      PublishDisabled
    }
  }

}
