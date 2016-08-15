package models

import com.gu.contentatom.thrift._
import com.gu.scanamo.{Table, _}
import db.ExplainerDB
import shared._
import javax.inject.Inject

import com.gu.contentatom.thrift.atom.explainer.{DisplayType, ExplainerAtom}
import config.Config

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.joda.time.DateTime
import util.ExplainerAtomImplicits
import com.gu.pandomainauth.model.{User => PandaUser}

class ExplainerStore @Inject() (config: Config) extends ExplainerAtomImplicits  {

  val explainerDB = new ExplainerDB(config)

  def buildAtomWithDefaults(id: String, explainerAtom: ExplainerAtom, contentChangeDetails: ContentChangeDetails): Atom = {
    Atom(
      id = id,
      atomType = AtomType.Explainer,
      defaultHtml = "-",
      data = AtomData.Explainer(explainerAtom),
      contentChangeDetails = contentChangeDetails
    )
  }

  def pandaUserToAtomUser(user: PandaUser): Option[User] = {
    Some(User(user.email,Some(user.firstName),Some(user.lastName)))
  }

  def contentChangeDetailsBuilder(user: PandaUser, existingContentChangeDetails: Option[ContentChangeDetails], updateCreated: Boolean,
    updateLastModified: Boolean, updatePublished: Boolean): ContentChangeDetails = {
    def buildChangeRecord(existingRecord: Option[ChangeRecord], shouldUpdate: Boolean) = {
      if (shouldUpdate) {
        Some(ChangeRecord(DateTime.now.getMillis, user=pandaUserToAtomUser(user)))
      } else if (existingRecord.isDefined) existingRecord else None
    }
    ContentChangeDetails(
      created      = buildChangeRecord(existingContentChangeDetails.flatMap(_.created)     , updateCreated),
      lastModified = buildChangeRecord(existingContentChangeDetails.flatMap(_.lastModified), updateLastModified),
      published    = buildChangeRecord(existingContentChangeDetails.flatMap(_.published)   , updatePublished),
      revision     = existingContentChangeDetails.map(_.revision).getOrElse(0L) + 1
    )
  }

  def update(id: String, fieldSymbol: Symbol, value: String, user: PandaUser): Future[Atom] = {
    val allowed_fields = Set(
      "title",
      "body",
      "displayType",
      "addTag",
      "removeTag"
    )
    assert(allowed_fields.contains(fieldSymbol.name))
    explainerDB.load(id).map{ explainer =>
      val newExplainerAtom = fieldSymbol.name match {
        case "title" => explainer.tdata.copy( title = value )
        case "body" => explainer.tdata.copy( body = value )
        case "displayType" => {
          value match {
            case "Expandable" => explainer.tdata.copy( displayType = DisplayType.Expandable )
            case "Flat" => explainer.tdata.copy( displayType = DisplayType.Flat )
          }
        }
        case "addTag" => explainer.tdata.copy(tags = Some((value +: explainer.tdata.tags.getOrElse(List())).distinct.sorted))
        case "removeTag" => explainer.tdata.copy(tags = explainer.tdata.tags.flatMap(tagList => {
          val newTagList = (tagList diff List(value)).sorted
          if(newTagList.isEmpty){ None }else{ Some(newTagList) }
        }))
      }
      val updatedExplainer = explainer.copy(
        data = AtomData.Explainer(newExplainerAtom),
        contentChangeDetails = contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails), updateCreated = false, updateLastModified = true, updatePublished = false)
      )
      explainerDB.update(updatedExplainer)
      updatedExplainer
    }
  }

  def create(user: PandaUser): Future[Atom] = {
    val uuid = java.util.UUID.randomUUID.toString
    val explainerAtom = ExplainerAtom("", "", DisplayType.Expandable)
    val contentChangeDetails = contentChangeDetailsBuilder(user, None, updateCreated = true, updateLastModified = true, updatePublished = false)
    val explainer = buildAtomWithDefaults(uuid, explainerAtom, contentChangeDetails)
    explainerDB.create(explainer)
    Future(explainer)
  }

  def publish(id: String, user: PandaUser): Future[Atom] = {
    explainerDB.load(id).map{ explainer =>
      val contentChangeDetails = contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails), updateCreated = false, updateLastModified = false, updatePublished = true)
      val updatedExplainer = explainer.copy(contentChangeDetails=contentChangeDetails)
      explainerDB.update(updatedExplainer)
      updatedExplainer
    }
  }

  //TODO: Takedowns are not currently supported for atoms in CAPI. This logic might be useful once we decided what to do about them!
//  def takeDown(id: String, user:PandaUser): Future[Atom] = {
//    explainerDB.load(id).map( explainer => {
//      val contentChangeDetails = contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails), updateCreated = false, updateLastModified = true, updatePublished = false, removePublished = true)
//      val updatedExplainer = explainer.copy(contentChangeDetails=contentChangeDetails)
//      explainerDB.update(updatedExplainer)
//      updatedExplainer
//    })
//  }

}

