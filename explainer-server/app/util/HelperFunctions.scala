package util

import com.gu.contentatom.thrift.{ChangeRecord, _}
import com.gu.contentatom.thrift.atom.explainer.{ExplainerAtom, DisplayType => ThriftDisplayType}
import org.joda.time.DateTime
import shared.models.{Available, Draft, PublicationStatus, TakenDown, UnlaunchedChanges}
import com.gu.pandomainauth.model.{User => PandaUser}
import com.gu.contentatom.thrift.User
import db.ExplainerDB
import play.api.libs.ws.{WSClient, WSResponse}
import shared.models.ExplainerUpdate
import shared.models.UpdateField._

import scala.concurrent.Future


object HelperFunctions {
  def getCreatedByString(explainer: Atom) = {
    val firstNameLastName = for {
      created <- explainer.contentChangeDetails.created
      user <- created.user
      firstName <- user.firstName
      lastName <- user.lastName
    } yield {
      s"$firstName $lastName"
    }
    firstNameLastName.getOrElse("-")
  }

  def getExplainerStatus(explainer: Atom, explainerDB: ExplainerDB): PublicationStatus = {
    val status = for {
      p <- explainer.contentChangeDetails.published
      lm <- explainer.contentChangeDetails.lastModified
    } yield {
      val inLiveTable = explainerDB.loadLive(explainer.id).isDefined
      if (inLiveTable) {
        if (p.date >= lm.date) Available else UnlaunchedChanges
      }
      else TakenDown
    }
    status.getOrElse(Draft)
  }

  def pandaUserToAtomUser(user: PandaUser): User = {
    User(user.email, Some(user.firstName), Some(user.lastName))
  }

  def contentChangeDetailsBuilder(user: PandaUser, existingContentChangeDetails: Option[ContentChangeDetails], updateCreated: Boolean = false,
    updateLastModified: Boolean = false, updatePublished: Boolean = false): ContentChangeDetails = {
    def buildChangeRecord(existingRecord: Option[ChangeRecord], shouldUpdate: Boolean) = {
      if (shouldUpdate) {
        Some(ChangeRecord(DateTime.now.getMillis, user=Some(pandaUserToAtomUser(user))))
      } else if (existingRecord.isDefined) existingRecord else None
    }
    ContentChangeDetails(
      created      = buildChangeRecord(existingContentChangeDetails.flatMap(_.created)     , updateCreated),
      lastModified = buildChangeRecord(existingContentChangeDetails.flatMap(_.lastModified), updateLastModified),
      published    = buildChangeRecord(existingContentChangeDetails.flatMap(_.published)   , updatePublished),
      revision     = existingContentChangeDetails.map(_.revision).getOrElse(0L) + 1
    )
  }

  def applyExplainerUpdate(explainer: ExplainerAtom, update: ExplainerUpdate): AtomData.Explainer = {
    val updatedExplainerAtom = update.field match {
      case Title => explainer.copy( title = update.value )
      case Body => explainer.copy( body = update.value )
      case DisplayType =>
        update.value match {
          case "Expandable" => explainer.copy( displayType = ThriftDisplayType.Expandable )
          case "Flat" => explainer.copy( displayType = ThriftDisplayType.Flat )
        }
      case AddTag => explainer.copy(tags = Some((update.value +: explainer.tags.getOrElse(List())).distinct.sorted))
      case RemoveTag => explainer.copy(tags = explainer.tags.flatMap(tagList => {
        val newTagList = (tagList diff List(update.value)).sorted
        if(newTagList.isEmpty) None else Some(newTagList)
      }))
    }
    AtomData.Explainer(updatedExplainerAtom)
  }

  def sendFastlyPurgeRequest(explainerId: String)(ws: WSClient): Future[WSResponse] = {
    val purgeRequest = ws.url(s"https://explainers-api.guim.co.uk/atom/explainer/$explainerId").withMethod("PURGE")
    purgeRequest.execute()
  }
}