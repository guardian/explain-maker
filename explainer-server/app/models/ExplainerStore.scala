package models

import com.gu.contentatom.thrift._
import com.gu.scanamo.{Table, _}
import contentatom.explainer.{DisplayType, ExplainerAtom}
import db.ExplainerDB
import shared._
import javax.inject.Inject

import config.Config

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.joda.time.DateTime
import shared.util.ExplainerAtomImplicits

import com.gu.pandomainauth.model.{User => PandaUser}

class ExplainerStore @Inject() (config: Config) extends ExplainerAtomImplicits  {

  val explainerDB = new ExplainerDB(config)

  def buildAtomWithDefaults(id: String, explainerAtom: ExplainerAtom, contentChangeDetails: ContentChangeDetails, user: PandaUser): Atom = {
    Atom(
      id = id,
      atomType = AtomType.Explainer,
      defaultHtml = "",
      data = AtomData.Explainer(explainerAtom),
      contentChangeDetails = contentChangeDetails
    )
  }

  def pandaUserToAtomUser(user: PandaUser): Option[User] = {
    Some(User(user.email,Some(user.firstName),Some(user.lastName)))
  }

  def contentChangeDetailsBuilder(user: PandaUser, existingContentChangeDetails: Option[ContentChangeDetails], updateCreated: Boolean, updateLastModified: Boolean, updatePublished: Boolean): ContentChangeDetails = {
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
      "displayType"
    )
    assert(allowed_fields.contains(fieldSymbol.name))
    explainerDB.load(id).map{ explainer =>
      val newExplainerAtom = fieldSymbol.name match {
        case "title" => ExplainerAtom(value, explainer.tdata.body, explainer.tdata.displayType)
        case "body" => ExplainerAtom(explainer.tdata.title, value, explainer.tdata.displayType)
        case "displayType" => {
          value match {
            case "Expandable" => ExplainerAtom(explainer.tdata.title, explainer.tdata.body, DisplayType.Expandable)
            case "Flat" => ExplainerAtom(explainer.tdata.title, explainer.tdata.body, DisplayType.Flat)
          }
        }
      }
      val contentChangeDetails = contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails), false, true, false)
      val updatedExplainer = buildAtomWithDefaults(explainer.id, newExplainerAtom, contentChangeDetails, user)
      explainerDB.store(updatedExplainer)
      updatedExplainer
    }
  }

  def create(user: PandaUser): Future[Atom] = {
    val uuid = java.util.UUID.randomUUID.toString
    val explainerAtom = ExplainerAtom("-", "-", DisplayType.Expandable)
    val contentChangeDetails = contentChangeDetailsBuilder(user, None, true, true, false)
    val explainer = buildAtomWithDefaults(uuid, explainerAtom, contentChangeDetails, user)
    explainerDB.store(explainer)
    Future(explainer)
  }

  def publish(id: String, user: PandaUser): Future[Atom] = {
    explainerDB.load(id).map{ explainer =>
      val contentChangeDetails = contentChangeDetailsBuilder(user, Some(explainer.contentChangeDetails), false, false, true)
      val updatedExplainer = buildAtomWithDefaults(explainer.id, explainer.tdata, contentChangeDetails, user)
      explainerDB.store(updatedExplainer)
      updatedExplainer
    }
  }

}

