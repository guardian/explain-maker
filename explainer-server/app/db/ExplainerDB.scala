package db

import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.data.Xor
import com.gu.contentatom.thrift._
import config.Config
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import com.gu.atom.data.{DataStoreResult, PreviewDynamoDataStore, PublishedDynamoDataStore}
import com.gu.contentatom.thrift.atom.explainer._
import shared.util.ExplainerAtomImplicits
import com.gu.atom.data.ScanamoUtil._
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.query.UniqueKeys
import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import shared.models.{WorkflowData, WorkflowStatus}
import util.HelperFunctions


class ExplainerDB @Inject() (config: Config) extends ExplainerAtomImplicits {

  val previewDynamoDataStore = new PreviewDynamoDataStore[ExplainerAtom](config.dynamoClient, config.previewTableName) {
    def fromAtomData = { case AtomData.Explainer(data) => data }
    def toAtomData(data: ExplainerAtom) = AtomData.Explainer(data)
  }

  val liveDynamoDataStore = new PublishedDynamoDataStore[ExplainerAtom](config.dynamoClient, config.liveTableName) {
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

  def stringConvertIterator(i: Iterator[Atom]) = i.toSeq.map(emptyStringConversion(_, emptyStringMarkerToEmptyString))

  def create(explainer: Atom) = {
    val sanitisedAtom = emptyStringConversion(explainer, emptyStringToEmptyStringMarker)
    previewDynamoDataStore.createAtom(sanitisedAtom)
  }

  def update(explainer: Atom): Unit = {
    val sanitisedAtom = emptyStringConversion(explainer, emptyStringToEmptyStringMarker)
    previewDynamoDataStore.updateAtom(sanitisedAtom)
  }

  // TODO: Switch to using dynamo async library or stop these functions from returning futures
  def all : Future[Seq[Atom]] = {
    Future(previewDynamoDataStore.listAtoms match {
      case Xor.Right(atoms) => stringConvertIterator(atoms)
      case _ => Nil
    })
  }

  def allLive: Future[Seq[Atom]] = {
    Future(liveDynamoDataStore.listAtoms match {
      case Xor.Right(atoms) => stringConvertIterator(atoms)
      case _ => Nil
    })
  }

  def load(id: String): Future[Atom] = {
    val explainer = previewDynamoDataStore.getAtom(id).get

    val sanitisedExplainer = emptyStringConversion(explainer, emptyStringMarkerToEmptyString)
    Future(sanitisedExplainer)

  }

  def loadLive(id: String): Option[Atom] = {
    val explainer = liveDynamoDataStore.getAtom(id)
    val sanitisedExplainer = explainer.map(e => emptyStringConversion(e, emptyStringMarkerToEmptyString))
    sanitisedExplainer
  }

  def publish(explainer: Atom) = {
    val sanitisedAtom = emptyStringConversion(explainer, emptyStringToEmptyStringMarker)
    liveDynamoDataStore.updateAtom(sanitisedAtom)
  }

  def takeDown(explainer: Atom) = {
    Scanamo.delete(config.dynamoClient)(config.liveTableName)('id -> explainer.id)
  }

  implicit val workflowStatusFormat = DynamoFormat.coercedXmap[WorkflowStatus, String, IllegalArgumentException](
    WorkflowStatus(_))(_.toString)

  val workflowDataTable = Table[WorkflowData](config.workflowDataTableName)
  def exec[A](ops: ScanamoOps[A]): A = Scanamo.exec(config.dynamoClient)(ops)

  def getWorkflowData(id: String): WorkflowData = {
    val defaultData = WorkflowData(id)
    exec(workflowDataTable.get('id -> id)).fold(defaultData)({
      case Xor.Right(data) => data
      case _ => defaultData
    })
  }

  def setWorkflowData(workflowData: WorkflowData) = {
    exec(workflowDataTable.put(workflowData))
  }

  def getWorkflowData(ids: List[String]) = {
    exec(workflowDataTable.getAll('id -> ids)).map(_.getOrElse(None)).asInstanceOf[List[WorkflowData]]
  }


}
