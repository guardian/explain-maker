package models

import com.gu.contentatom.thrift._
import com.gu.scanamo.{Table, _}
import com.gu.scanamo.syntax._
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import contentatom.explainer.{DisplayType, ExplainerAtom}
import db.ExplainerDB
import shared._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.joda.time.DateTime
import shared.util.ExplainerAtomImplicits
import db.ExplainerDB.{dynamoDBClient, explainersTable, seqFormat}

object ExplainerStore extends ExplainerAtomImplicits  {

  def buildAtomWithDefaults(id: String, explainerAtom: ExplainerAtom, revision: Long): Atom = {
    Atom(
      id = id,
      atomType = AtomType.Explainer,
      defaultHtml = "",
      data = AtomData.Explainer(explainerAtom),
      contentChangeDetails = ContentChangeDetails(lastModified = Some(ChangeRecord(DateTime.now.getMillis, user=None)), revision = revision)
    )
  }

  def update(id: String, fieldSymbol: Symbol, value: String): Future[Atom] = {
    val allowed_fields = Set(
      "title",
      "body",
      "displayType"
    )
    assert(allowed_fields.contains(fieldSymbol.name))
    ExplainerDB.load(id).map{ explainer =>
      val newExplainerAtom = fieldSymbol.name match {
        case "title" => ExplainerAtom(value, explainer.tdata.body, explainer.tdata.displayType)
        case "body" => ExplainerAtom(explainer.tdata.title, value, explainer.tdata.displayType)
//        case "displayType" => ExplainerAtom(explainer.tdata.title, explainer.tdata.body, DisplayType.Expandable)
      }
      val updatedExplainer = buildAtomWithDefaults(explainer.id, newExplainerAtom, explainer.contentChangeDetails.revision+1)
      ExplainerDB.store(updatedExplainer)
      updatedExplainer
    }
  }

  def create(): Future[Atom] = {
    val uuid = java.util.UUID.randomUUID.toString
    val explainerAtom = ExplainerAtom("-", "-", DisplayType.Expandable)
    val explainer = buildAtomWithDefaults(uuid, explainerAtom, 1)
    ExplainerDB.store(explainer)
    Future(explainer)
  }
}

