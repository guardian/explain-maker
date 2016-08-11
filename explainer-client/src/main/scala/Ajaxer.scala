import autowire.Macros.Check
import autowire._

import common.ExtAjax._
import org.scalajs.dom.ext.Ajax
import rx._
import shared.ExplainerApi
import shared.models.{CsAtom, ExplainerUpdate}

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

  def extractExplainer(id: String): Future[CsAtom] = {
    Ajaxer[ExplainerApi].load(id).call()
  }

  def updateFieldContent(id: String, explainer: ExplainerUpdate) = {
    Ajaxer[ExplainerApi].update(id, explainer.field, explainer.value).call()
  }

  def createNewExplainer() = {
    Ajaxer[ExplainerApi].create().call()
  }

  def publish(id: String): Future[CsAtom] = {
    Ajaxer[ExplainerApi].publish(id).call()
  }

  def addTagToExplainer(explainerId: String, tagId: String): Future[CsAtom] = {
    Ajaxer[ExplainerApi].update(explainerId, "addTag", tagId).call()
  }

  def removeTagFromExplainer(explainerId: String, tagId: String): Future[CsAtom] = {
    Ajaxer[ExplainerApi].update(explainerId, "removeTag", tagId).call()
  }

}