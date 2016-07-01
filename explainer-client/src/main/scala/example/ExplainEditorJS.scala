package example

import common.Framework
import config.Routes
import org.scalajs.dom
import org.scalajs.dom.ext.{Ajax, AjaxException}
import org.scalajs.dom.html.{Input, TextArea}
import rx._
import shared._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom._
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.section


@JSExport
object ExplainEditorJS {
  import Framework._

  object Model {
    import scala.scalajs.js
    import js.Dynamic.{global => g}
    import org.scalajs.jquery.{jQuery=>$}
    import upickle.default._
    import common.ExtAjax._

    val tasks = Var(List.empty[Task])

    val done = Rx{ tasks().count(_.done)}

    val notDone = Rx{ tasks().length - done()}

    val editing = Var[Option[Task]](None)

    val filter = Var("All")

    val filters = Map[String, Task => Boolean](
      ("All", t => true),
      ("Active", !_.done),
      ("Completed", _.done)
    )

    def all: List[Task] = tasks()

    def saveContent(field: String, value: String) = {
      val json = s"""{"$field": "$value"}"""
      val id = 123
      Ajax.postAsJson(Routes.ExplainEditor.update(id), json).map{ r =>
        if(r.ok){
          // celebrate in some way
        }
      }
    }

  }

  val headline: Input = input(
    id:="new-todo",
    placeholder:="headline",
    autofocus:=true,
    onchange := { () =>
      Model.saveContent("headline", headline.value)
      false
    }
  ).render

  val body: TextArea = textarea(
    id:="new-todo",
    placeholder:="body",
    autofocus:=true,
    onchange := { () =>
      Model.saveContent("body", body.value)
      false
    }
  ).render

  def templateHeader = {
    header(id:="header")(
      form(
        headline, body
      )
    )
  }

  def templateBody = {
    section(id:="main")(
      input(
        id:="toggle-all",
        `type`:="checkbox",
        cursor:="pointer",
        onclick := { () =>
          val target = Model.tasks().exists(_.done == false)
//          Var.set(tasks().map(_.done -> target): _*)
        }
      ),
      label(`for`:="toggle-all", "Mark all as complete")
    )
  }

  @JSExport
  def main(): Unit = {
    dom.document.getElementById("content").appendChild(
      section(id:="todoapp")(
        templateHeader,
        templateBody
      ).render
    )
  }


}