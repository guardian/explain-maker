import autowire.Macros.Check
import autowire._
import common.ExtAjax._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.{Element, Input, Option, TextArea}
import org.scalajs.dom.{Event, FocusEvent}
import presence.StateChange.State
import presence.{Person, PresenceGlobalScope, StateChange}
import rx._
import shared._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom._
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.section
import shared.models.{CsAtom, ExplainerUpdate}
import shared.models.CsAtom._
import upickle.Js
import upickle.default._

@JSExport
object ExplainEditorJS {

  @JSExport
  def main(explainerId: String) = {
    val articleId = "explain-"+explainerId

//    ExplainEditorPresenceHelpers.presenceClient.startConnection()
//
//    ExplainEditorPresenceHelpers.presenceClient.on("connection.open", { data:js.Object =>
//      ExplainEditorPresenceHelpers.presenceClient.subscribe(articleId)
//    })

    Model.extractExplainer(explainerId).map { explainer: CsAtom =>
      dom.document.getElementById("content").appendChild(
        ExplainEditorJSDomBuilders.ExplainEditor(explainerId, explainer).render
      )
      ExplainEditorJSDomBuilders.republishStatusBar(explainer)
      g.updateWordCountDisplay()
      g.updateWordCountWarningDisplay()
      g.initiateEditor()
    }
  }

  @JSExport
  def generateExplainerTagManagement(explainerId: String): Future[String] = {
    Model.extractExplainer(explainerId).map( explainer => ExplainEditorJSDomBuilders.makeTagArea(explainer).render.outerHTML )
  }

  @JSExport
  def CreateNewExplainer() = {
    Model.createNewExplainer().map{ explainer: CsAtom =>
      g.location.href = s"/explain/${explainer.id}"
    }
  }

  @JSExport
  def setDisplayType(explainerId: String, displayType: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate("displayType", displayType))
  }

  @JSExport
  def updateBodyContents(explainerId: String, bodyString: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate("body", bodyString)).map(ExplainEditorJSDomBuilders.republishStatusBar)
  }

  @JSExport
  def addTagToExplainer(explainerId: String, tagId: String) = {
    Model.addTagToExplainer(explainerId, tagId).map { explainer =>
      ExplainEditorJSDomBuilders.redisplayExplainerTagManagementAreas(explainer.id)
    }
  }

  @JSExport
  def removeTagFromExplainer(explainerId: String, tagId: String) = {
    Model.removeTagFromExplainer(explainerId, tagId).map { explainer =>
      ExplainEditorJSDomBuilders.redisplayExplainerTagManagementAreas(explainer.id)
    }
  }

  @JSExport
  def addTagToSuggestionSet(explainerId: String, divIdentifier: String, tagId: String, userInterfaceDescription: String) = {
    val node = div(cls:="tag__result")(userInterfaceDescription).render
    node.onclick = (x: Event) => {
      addTagToExplainer(explainerId, tagId)
    }
    dom.document.getElementById(divIdentifier).appendChild(node)
  }

}