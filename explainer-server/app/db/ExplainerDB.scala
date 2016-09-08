package db

import javax.inject.Inject

import cats.data.Xor
import com.gu.contentatom.thrift._
import config.Config
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.gu.atom.data.DynamoDataStore
import com.gu.atom.data.ScanamoUtil._
import com.gu.contentatom.thrift.atom.explainer._
import shared.util.ExplainerAtomImplicits


class ExplainerDB @Inject() (config: Config) extends ExplainerAtomImplicits {

  val dynamoDataStore = new DynamoDataStore[ExplainerAtom](config.dynamoClient, config.tableName) {
    def fromAtomData = { case AtomData.Explainer(data) => data }
    def toAtomData(data: ExplainerAtom) = AtomData.Explainer(data)
  }
  def emptyStringMarkerToEmptyString(s: String) = if (s == "-") "" else s
  def emptyStringToEmptyStringMarker(s: String) = if (s == "") "-" else s

  private def emptyStringConversion(explainer: Atom, conversionFunction: String => String) = {
    explainer.copy(data = AtomData.Explainer(
      explainer.tdata.copy(
        title=conversionFunction(explainer.tdata.title),
        body = conversionFunction(explainer.tdata.body))))
  }

  def create(explainer: Atom) = {
    val sanitisedAtom = emptyStringConversion(explainer, emptyStringToEmptyStringMarker)
    dynamoDataStore.createAtom(sanitisedAtom)
  }

  def update(explainer: Atom): Unit = {
    val sanitisedAtom = emptyStringConversion(explainer, emptyStringToEmptyStringMarker)
    dynamoDataStore.updateAtom(sanitisedAtom)
  }

  // TODO: Switch to using dynamo async library or stop these functions from returning futures
  def all : Future[Seq[Atom]] = {
    Future(dynamoDataStore.listAtoms match {
      case Xor.Right(atoms) => atoms.toSeq.map(emptyStringConversion(_, emptyStringMarkerToEmptyString))
      case _ => Nil
    })
  }

  def load(id: String): Future[Atom] = {
    val explainer = dynamoDataStore.getAtom(id).get

    val sanitisedExplainer = emptyStringConversion(explainer, emptyStringMarkerToEmptyString)
    Future(sanitisedExplainer)

  }

}
