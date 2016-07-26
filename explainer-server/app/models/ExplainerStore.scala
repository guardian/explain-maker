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
      val contentChangeDetails = ContentChangeDetails(
        created      = explainer.contentChangeDetails.created,
        lastModified = Some(ChangeRecord(DateTime.now.getMillis, user=Some(User(user.email,Some(user.firstName),Some(user.lastName))))),
        published    = explainer.contentChangeDetails.published,
        revision = explainer.contentChangeDetails.revision+1
      )
      val updatedExplainer = buildAtomWithDefaults(explainer.id, newExplainerAtom, contentChangeDetails, user)
      explainerDB.store(updatedExplainer)
      updatedExplainer
    }
  }

  def create(user: PandaUser): Future[Atom] = {
    val uuid = java.util.UUID.randomUUID.toString
    val explainerAtom = ExplainerAtom("-", "-", DisplayType.Expandable)
    val contentChangeDetails = ContentChangeDetails(
      created      = Some(ChangeRecord(DateTime.now.getMillis, user=Some(User(user.email,Some(user.firstName),Some(user.lastName))))),
      lastModified = Some(ChangeRecord(DateTime.now.getMillis, user=Some(User(user.email,Some(user.firstName),Some(user.lastName))))),
      published    = None,
      revision = 1
    )
    val explainer = buildAtomWithDefaults(uuid, explainerAtom, contentChangeDetails, user)
    explainerDB.store(explainer)
    Future(explainer)
  }

  def publish(id: String, user: PandaUser): Future[Atom] = {
    explainerDB.load(id).map{ explainer =>
      val newExplainerAtom = ExplainerAtom(explainer.tdata.title, explainer.tdata.body, explainer.tdata.displayType)
      val contentChangeDetails = ContentChangeDetails(
        created      = explainer.contentChangeDetails.created,
        lastModified = explainer.contentChangeDetails.lastModified,
        published    = Some(ChangeRecord(DateTime.now.getMillis, user=Some(User(user.email,Some(user.firstName),Some(user.lastName))))),
        revision     = explainer.contentChangeDetails.revision+1
      )
      val updatedExplainer = buildAtomWithDefaults(explainer.id, newExplainerAtom, contentChangeDetails, user)
      explainerDB.store(updatedExplainer)
      updatedExplainer
    }
  }

}

