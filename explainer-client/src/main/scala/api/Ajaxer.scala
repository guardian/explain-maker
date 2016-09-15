package api

import autowire._
import common.ExtAjax._
import org.scalajs.dom.ext.Ajax
import rx._
import shared.ExplainerApi
import shared.models.PublicationStatus.PublicationStatus
import shared.models.UpdateField.{AddTag, RemoveTag}
import shared.models.{CsAtom, ExplainerUpdate, WorkflowData}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import upickle.Js
import upickle.default._

object Ajaxer extends autowire.Client[Js.Value, Reader, Writer]{
  override def doCall(req: Request): Future[Js.Value] = {
    Ajax.postAsJson(
      "/api/" + req.path.mkString("/"),
      upickle.json.write(Js.Obj(req.args.toSeq:_*))
    ).map(_.responseText)
      .map(upickle.json.read)
  }
  def read[Result: Reader](p: Js.Value) = readJs[Result](p)
  def write[Result: Writer](r: Result) = writeJs(r)
}

object Model {

  def getExplainer(id: String): Future[CsAtom] = {
    Ajaxer[ExplainerApi].load(id).call()
  }

  def updateFieldContent(id: String, explainerUpdate: ExplainerUpdate) = {
    Ajaxer[ExplainerApi].update(id, explainerUpdate).call()
  }

  def createNewExplainer() = {
    Ajaxer[ExplainerApi].create().call()
  }

  def publish(id: String): Future[CsAtom] = {
    Ajaxer[ExplainerApi].publish(id).call()
  }

  def takeDown(id: String): Future[CsAtom] = {
    Ajaxer[ExplainerApi].takeDown(id).call()
  }

  def getExplainerStatus(explainerId: String, checkCapiStatus: Boolean): Future[PublicationStatus] = {
    Ajaxer[ExplainerApi].getStatus(explainerId, checkCapiStatus).call()
  }

  def getWorkflowData(id: String): Future[WorkflowData] = {
    Ajaxer[ExplainerApi].getWorkflowData(id).call()
  }

  def setWorkflowData(workflowData: WorkflowData) = {
    Ajaxer[ExplainerApi].setWorkflowData(workflowData).call()
  }

}