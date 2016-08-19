import org.scalajs.dom
import shared.models.{CsAtom, ExplainerUpdate}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport

@JSExport
object ExplainEditorJS {

  @JSExport
  def main(explainerId: String, callback: js.Function0[Unit]) = {
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

      dom.document.getElementById("sidebar").appendChild(
        ExplainEditorJSDomBuilders.SideBar(explainerId, explainer).render
      )
      ExplainEditorJSDomBuilders.republishStatusBar(explainer)
      callback()
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
  def publish(explainerId: String) = {
    Model.publish(explainerId).map(ExplainEditorJSDomBuilders.republishStatusBar)
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
  def removeTagFromExplainer(explainerId: String, tagId: String) = {
    Model.removeTagFromExplainer(explainerId, tagId).map { explainer =>
      ExplainEditorJSDomBuilders.redisplayExplainerTagManagementAreas(explainer.id)
    }
  }

}