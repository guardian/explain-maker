package components.explaineditor

import api.Model
import components.statusbar.StatusBar
import org.scalajs.dom
import shared.models.{CsAtom, ExplainerUpdate}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport
import scala.util.{Failure, Success}

@JSExport
object ExplainEditorJS {

  def updateEmbedUrlAndStatusLabel(id: String, checkCapi: Boolean = true) = {
      Model.getExplainerStatus(id, checkCapi).map(s => {
        StatusBar.updateStatusBar(s)
        ExplainEditorJSDomBuilders.republishembedURL(id, s)
      })
  }

  @JSExport
  def main(explainerId: String, callback: js.Function0[Unit]) = {

    if(g.CONFIG.PRESENCE_ENABLED.toString == "true") {
      ExplainEditorPresenceHelpers.presenceClient.startConnection()
      ExplainEditorPresenceHelpers.presenceClient.on("connection.open", { data:js.Object =>
        ExplainEditorPresenceHelpers.presenceClient.subscribe(s"explain-$explainerId")
      })
    }

    updateEmbedUrlAndStatusLabel(explainerId)
    Model.getExplainer(explainerId).map { explainer: CsAtom =>

      dom.document.getElementById("content").appendChild(
        ExplainEditorJSDomBuilders.ExplainEditor(explainer)
      )

      dom.document.getElementById("sidebar").appendChild(
        ExplainEditorJSDomBuilders.SideBar(explainer).render
      )
      callback()
    }
  }

  @JSExport
  def publish(explainerId: String) = {
    Model.publish(explainerId) onComplete {
      case Success(_) => updateEmbedUrlAndStatusLabel(explainerId)
      case Failure(_) => g.console.error(s"Failed to publish explainer")
    }
  }

  @JSExport
  def takeDown(explainerId: String) = {
    Model.takeDown(explainerId) onComplete {
      case Success(_) => updateEmbedUrlAndStatusLabel(explainerId)
      case Failure(_) => g.console.error(s"Failed to take down explainer")
    }
  }

  @JSExport
  def setDisplayType(explainerId: String, displayType: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate("displayType", displayType))
  }

  @JSExport
  def updateBodyContents(explainerId: String, bodyString: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate("body", bodyString)) onComplete {
      case Success(_) => updateEmbedUrlAndStatusLabel(explainerId, checkCapi=false)
      case Failure(_) => g.console.error(s"Failed to update body with string $bodyString")
    }
  }

  @JSExport
  def removeTagFromExplainer(explainerId: String, tagId: String) = {
    Model.removeTagFromExplainer(explainerId, tagId).map { explainer =>
      ExplainEditorJSDomBuilders.redisplayExplainerTagManagementAreas(explainer)
    }
  }

  @JSExport
  def presenceEnterDocument(explainerId: String) = {
    ExplainEditorPresenceHelpers.enterDocument(explainerId)
  }

}