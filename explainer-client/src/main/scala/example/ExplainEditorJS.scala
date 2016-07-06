package example

import autowire._
import common.ExtAjax._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.{Input, TextArea}
import rx._
import shared._
import upickle.Js
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom._
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.section

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


@JSExport
object ExplainEditorJS {

  object Model {
    import org.scalajs.jquery.{jQuery => $}

    val explainer = Var(Explainer)

    def init(id: String): Future[Explainer] = {
      Ajaxer[ExplainerApi].load(id).call()
    }

    def saveContent(id: String, explainer: ExplainerUpdate) = {
      Ajaxer[ExplainerApi].update(id, explainer.field, explainer.value).call()
    }

  }

  val headline: TypedTag[Input] = input(
    id:="new-atom",
    placeholder:="headline",
    autofocus:=true
  )

  val body: TypedTag[TextArea] = textarea(
    id:="new-atom",
    rows:=18,
    cols:=50,
    maxlength:=1800,
    placeholder:="body",
    autofocus:=true
  )

  def templateHeader(explainerId: String, explainer: Explainer) = {

    val headlineTag = headline(value := explainer.headline).render
    headlineTag.onchange = (x: Event) => {
      Model.saveContent(explainerId, ExplainerUpdate("headline", headlineTag.value))
      false
    }

    val bodyTag = body(explainer.body).render
    bodyTag.onchange = (x: Event) => {
      Model.saveContent(explainerId, ExplainerUpdate("body", bodyTag.value))
      false
    }


    header(id:="header")(
      form(
        headlineTag,
        bodyTag
      )
    )
  }

  @JSExport
  def main(explainerId: String) = {

    Model.init(explainerId).map { explainer: Explainer =>
      dom.document.getElementById("content").appendChild(
        section(id:="Explainer Editor")(
          templateHeader(explainerId, explainer)
        ).render
      )
    }
  }

}