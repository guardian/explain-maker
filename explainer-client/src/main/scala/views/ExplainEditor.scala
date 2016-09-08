package views

import api.Model
import components.statusbar.StatusBar
import components.{ScribeBodyEditor, Sidebar, TagPickers}
import org.scalajs.dom
import services.PresenceClient
import shared.models.{CsAtom, ExplainerUpdate}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport
import scala.util.{Failure, Success}
import scala.scalajs.js.{Function0, Object => JsObject}

@JSExport
object ExplainEditor {

  @JSExport
  def main(explainerId: String, callback: Function0[Unit]) = {

    if(g.CONFIG.PRESENCE_ENABLED.toString == "true") {
      PresenceClient.presenceClient.startConnection()
      PresenceClient.presenceClient.on("connection.open", { data:JsObject =>
        PresenceClient.presenceClient.subscribe(s"explain-$explainerId")
      })
    }

    updateEmbedUrlAndStatusLabel(explainerId)
    Model.getExplainer(explainerId).map { explainer: CsAtom =>

      dom.document.getElementById("content").appendChild(
        ScribeBodyEditor.renderedBodyEditor(explainer)
      )

      dom.document.getElementById("sidebar").appendChild(
        Sidebar.sidebar(explainer)
      )
      callback()
    }
  }

  def updateEmbedUrlAndStatusLabel(id: String, checkCapi: Boolean = true) = {
    Model.getExplainerStatus(id, checkCapi).map(s => {
      StatusBar.updateStatusBar(s)
      Sidebar.republishembedURL(id, s)
    })
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
      TagPickers.redisplayExplainerTagManagementAreas(explainer)
    }
  }

  @JSExport
  def presenceEnterDocument(explainerId: String) = {
    PresenceClient.enterDocument(explainerId)
  }

}