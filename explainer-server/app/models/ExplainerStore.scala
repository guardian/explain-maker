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

class ExplainerStore @Inject() (config: Config) extends ExplainerAtomImplicits  {

  val explainerDB = new ExplainerDB(config)

  def buildAtomWithDefaults(user: com.gu.pandomainauth.model.User, id: String, explainerAtom: ExplainerAtom, revision: Long): Atom = {
    Atom(
      id = id,
      atomType = AtomType.Explainer,
      defaultHtml = "",
      data = AtomData.Explainer(explainerAtom),
      contentChangeDetails = ContentChangeDetails(
        lastModified = Some(ChangeRecord(DateTime.now.getMillis, user=Some(User(user.email,Some(user.firstName),Some(user.lastName))))),
        revision = revision)
    )
  }

  def update(user: com.gu.pandomainauth.model.User, id: String, fieldSymbol: Symbol, value: String): Future[Atom] = {
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
      val updatedExplainer = buildAtomWithDefaults(user, explainer.id, newExplainerAtom, explainer.contentChangeDetails.revision+1)
      explainerDB.store(updatedExplainer)
      updatedExplainer
    }
  }

  def create(user: com.gu.pandomainauth.model.User): Future[Atom] = {
    val uuid = java.util.UUID.randomUUID.toString
    val explainerAtom = ExplainerAtom("-", "-", DisplayType.Expandable)
    val explainer = buildAtomWithDefaults(user, uuid, explainerAtom, 1)
    explainerDB.store(explainer)
    Future(explainer)
  }
}

