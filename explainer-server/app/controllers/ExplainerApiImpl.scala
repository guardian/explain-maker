package controllers

import com.gu.atom.data.{PreviewDataStore, PublishedDataStore}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}
import org.joda.time.DateTime
import play.api.cache.CacheApi
import play.api.Logger
import com.gu.atom.publish.{AtomPublisher, LiveAtomPublisher, PreviewAtomPublisher}
import com.gu.contentapi.client.model.v1.TagType
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.explainer.{ExplainerAtom, DisplayType => ThriftDisplayType}
import config.Config
import db.ExplainerDB
import models.{PublishResult, Disabled => PublishDisabled, Fail => PublishFail, Success => PublishSuccess}
import services.{CAPIService, PublicSettingsService}
import shared.ExplainerApi
import shared.models.{CsAtom, ExplainerUpdate, WorkflowData}
import shared.util.ExplainerAtomImplicits._
import com.gu.pandomainauth.model.{User => PandaUser}
import play.api.libs.ws.WSClient
import shared.models._
import util.HelperFunctions.{applyExplainerUpdate, contentChangeDetailsBuilder, getExplainerStatus}
import shared.util.SharedHelperFunctions.generateDefaultHtml
import util.NotingHelper


class ExplainerApiImpl(
  config: Config,
  previewAtomPublisher: PreviewAtomPublisher,
  liveAtomPublisher: LiveAtomPublisher,
  previewDynamoDataStore: PreviewDataStore,
  liveDynamoDataStore: PublishedDataStore,
  val publicSettingsService: PublicSettingsService,
  user: PandaUser,
  cache: CacheApi,
  ws: WSClient) extends ExplainerApi {

  val explainerDB = new ExplainerDB(config, previewDynamoDataStore, liveDynamoDataStore)
  val capiService = new CAPIService(config, cache)

  override def create(): Future[CsAtom] = {

    val atomData = AtomData.Explainer(ExplainerAtom("", "", ThriftDisplayType.Flat))

    val explainer = Atom(
      id = java.util.UUID.randomUUID.toString,
      atomType = AtomType.Explainer,
      defaultHtml = generateDefaultHtml(atomData.explainer),
      data = atomData,
      contentChangeDetails = contentChangeDetailsBuilder(user, None, updateCreated = true, updateLastModified = true)
    )
    explainerDB.create(explainer)
    setWorkflowData(WorkflowData(explainer.id))
    sendKinesisEvent(explainer, s"Publishing new explainer ${explainer.id} to PREVIEW kinesis", previewAtomPublisher)
    Future(CsAtom.atomToCsAtom(explainer))
  }

  override def load(id: String): Future[CsAtom] = explainerDB.load(id).map(CsAtom.atomToCsAtom)

  override def update(id: String, update: ExplainerUpdate): Future[CsAtom] = {
    explainerDB.load(id).map( explainer => {

      val updatedAtomData = applyExplainerUpdate(explainer.tdata, update)

      val updatedExplainer = explainer.copy(
        data = updatedAtomData,
        defaultHtml = generateDefaultHtml(updatedAtomData.explainer),
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
      explainerDB.publish(updatedExplainer)
      sendKinesisEvent(updatedExplainer, s"Publishing explainer ${updatedExplainer.id} to LIVE kinesis", liveAtomPublisher)
      sendFastlyPurgeRequest(id)
      CsAtom.atomToCsAtom(updatedExplainer)
    }
  }

  override def takeDown(id: String): Future[CsAtom] = {
    explainerDB.load(id).map( explainer => {
      val updatedExplainer = explainer.copy(
        contentChangeDetails=contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails), updateLastModified = true)
      )
      explainerDB.update(updatedExplainer)
      explainerDB.takeDown(updatedExplainer)
      sendKinesisEvent(updatedExplainer, s"Sending takedown event for explainer ${updatedExplainer.id} to LIVE kinesis.", liveAtomPublisher, EventType.Takedown)
      sendFastlyPurgeRequest(id)
      CsAtom.atomToCsAtom(updatedExplainer)
    })
  }

  override def getStatus(id:String): Future[PublicationStatus] = {
    for {
      e <- explainerDB.load(id)
    } yield {
      getExplainerStatus(e, explainerDB)
    }
  }

  override def getWorkflowData(id:String): WorkflowData = {
    explainerDB.getWorkflowData(id)
  }

  override def setWorkflowData(workflowData: WorkflowData) = {
    explainerDB.setWorkflowData(workflowData)
  }

  override def getTrackingTags(): Future[Seq[CsTag]] = {
    capiService.getTrackingTags.map{ tags =>
      tags.map(t => CsTag(t.id, t.webTitle, t.sectionName.getOrElse("unknown section"), t.`type`.name))
    }
  }

  def sendFastlyPurgeRequest(id: String): Unit = {
    Future {
      blocking {
        if (config.fastlyPurgingEnabled) {
          val purgeRequest = ws.url(s"https://explainers-api.guim.co.uk/atom/explainer/$id").withMethod("PURGE").withHeaders(("Fastly-Key", config.fastlyAPIKey))
          Thread.sleep(5000)
          purgeRequest.execute().foreach { r =>
            Logger.info(s"Fastly purge request result: ${r.status} ${r.statusText}, ${r.body}")
          }
        }
      }
    }
  }

  private def sendKinesisEvent(explainer: Atom, actionMessage: String, atomPublisher: AtomPublisher, eventType: EventType = EventType.Update): PublishResult = {
    if (config.publishToKinesis) {
      //Remove notes before sending to CAPI
      val cleanedExplainer = explainer.updateData((data) => {
        data.copy(
          body = NotingHelper.removeNotesAndUnwrapFlags(data.body)
        )
      })

      val event = ContentAtomEvent(cleanedExplainer, eventType, DateTime.now.getMillis)
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
