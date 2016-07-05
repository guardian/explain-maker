package example

import config.Routes
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.ext.{Ajax, AjaxException}
import org.scalajs.dom.html.{Input, TextArea}
import rx._
import shared._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom._
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.section



@JSExport
object ExplainEditorJS {

  object Model {
    import scala.scalajs.js
    import js.Dynamic.{global => g}
    import org.scalajs.jquery.{jQuery=>$}
    import upickle.default._
    import common.ExtAjax._

    val explainer = Var(Explainer)

    def init(id: String): Future[Explainer] = {
      Ajax.get(Routes.ExplainEditor.loadExplainer(id)).map { r =>
        read[Explainer](r.responseText)
      }
    }

    def saveContent(id: String, field: String, value: String) = {
      val json = s"""{"$field": "$value"}"""
      Ajax.postAsJson(Routes.ExplainEditor.update(id), json).map{ r =>
        if(r.ok){
          // celebrate in some way
        }
      }
    }

  }

  val headline: TypedTag[Input] = input(
    id:="new-todo",
    placeholder:="headline",
    autofocus:=true
  )

  val body: TypedTag[TextArea] = textarea(
    id:="new-todo",
    placeholder:="body",
    autofocus:=true
  )

  def templateHeader(explainerId: String, explainer: Explainer) = {

    val headlineTag = headline(value := explainer.headline).render
    headlineTag.onchange = (x: Event) => {
      Model.saveContent(explainerId ,"headline", headlineTag.value)
      false
    }

    val bodyTag = body(explainer.body).render
    bodyTag.onchange = (x: Event) => {
      Model.saveContent(explainerId, "body", bodyTag.value)
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