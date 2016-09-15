package util

import com.gu.contentatom.thrift.{ChangeRecord, _}
import com.gu.contentatom.thrift.atom.explainer.{DisplayType =>ThriftDisplayType, ExplainerAtom}
import org.joda.time.DateTime
import services.CAPIService
import shared.models.PublicationStatus._
import com.gu.pandomainauth.model.{User => PandaUser}
import com.gu.contentatom.thrift.User
import shared.models.ExplainerUpdate
import shared.models.UpdateField._

import scala.concurrent.Future
import shared.util.ExplainerAtomImplicits

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

  def isPublished(explainer: Atom) = {
    if(explainer.contentChangeDetails.published.isDefined) "Published" else "Draft"
  }

  // works out current status of explainer. If checkCapiStatus is provided then a query to capi will not be made - instead
  // the value of checkCapiStatus will be used
  def getExplainerStatus(explainer: Atom, capiService: CAPIService, checkCapiStatus: Boolean, stage: String): Future[PublicationStatus] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val status = for {
      p <- explainer.contentChangeDetails.published
      lm <- explainer.contentChangeDetails.lastModified
    } yield {
      def capiStatusToExplainerStatus(inCapi: Boolean) = {
        if (inCapi) {
          if (p.date >= lm.date) Available
          else UnlaunchedChanges
        }
        else TakenDown
      }
      // explain maker doesn't publish to capi when running locally
      val explainerInCapi= if (stage != "DEV" && checkCapiStatus) {
        capiService.checkExplainerInCapi(explainer.id)
      } else Future(true)
      explainerInCapi.map(capiStatusToExplainerStatus)
    }
    status.getOrElse(Future(Draft))
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

  def selectTagByIdFromTrackingTags(trackingTags:Seq[com.gu.contentapi.client.model.v1.Tag],id: String): Option[com.gu.contentapi.client.model.v1.Tag] = {
    trackingTags.filter(t => t.id == id).headOption
  }
  def selectFirstDeskOrEmptyString(trackingTags:Seq[com.gu.contentapi.client.model.v1.Tag], e: Atom): String = {
    ExplainerAtomImplicits.AtomWithData(e).tdata.tags match {
      case None => ""
      case Some(sequence) => {
        val elements = sequence.filter( s => s.startsWith("tracking/commissioningdesk") )
        if(elements.size>0){
          elements.head
          selectTagByIdFromTrackingTags(trackingTags,elements.head) match {
            case None => ""
            case Some(tag) => tag._5
          }
        }else {
          ""
        }
      }
    }
  }

}