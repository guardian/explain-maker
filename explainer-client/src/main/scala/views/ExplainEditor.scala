package views

import api.Model
import components.{ScribeBodyEditor, Sidebar, StatusBar, TagPickers}
import org.scalajs.dom
import org.scalajs.dom.html.Div
import services.{PresenceClient, State}
import shared.models.UpdateField.{Body, DisplayType, RemoveTag, UpdateField}
import shared.models.ExplainerUpdate

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport
import scala.util.{Failure, Success}
import scala.scalajs.js.{Function0, Object => JsObject}
import shared.models._
import shared.util.SharedHelperFunctions
import scala.scalajs.js.timers._


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

    val explainer =  Model.getExplainer(explainerId)

    explainer.map { explainer =>
      dom.document.getElementById("content").appendChild(
        ScribeBodyEditor.renderedBodyEditor(explainer)
    )
  }

    for {
      e <- explainer
      wfData <- Model.getWorkflowData(explainerId)
    } yield {
      Model.getExplainerStatus(explainerId).map { s =>
        if (s == TakenDown) State.takenDown = true
        dom.document.getElementById("sidebar").appendChild(
          Sidebar.sidebar(e, s, wfData.status)
        )
        updateEmbedUrlAndStatusLabel(explainerId, s)
      }
      callback()
    }

  }

  def updateFieldAndRefresh(explainerId: String, updateField: UpdateField, updateValue: String, errorMessage: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate(updateField, updateValue)) onComplete {
      case Success(e) => ExplainEditor.updateEmbedUrlAndStatusLabel(explainerId, SharedHelperFunctions.getExplainerStatusNoTakeDownCheck(e, State.takenDown))
      case Failure(_) => g.console.error(errorMessage)
    }
  }

  def updateEmbedUrlAndStatusLabel(id: String, status: PublicationStatus) = {
    StatusBar.updateStatusBar(status)
    Sidebar.republishembedURL(id, status)
    // show spinner for 4s to allow fastly cache to purge
    setTimeout(4000) {
      dom.document.getElementById("publication-state-icon").asInstanceOf[Div].classList.remove("state-indicator--loading")
    }
  }

  @JSExport
  def publish(explainerId: String) = {
    dom.document.getElementById("publication-state-icon").asInstanceOf[Div].classList.add("state-indicator--loading")
    Model.publish(explainerId) onComplete {
      case Success(_) =>
        State.takenDown = false
        updateEmbedUrlAndStatusLabel(explainerId, Available)
      case Failure(_) => g.console.error(s"Failed to publish explainer")
    }
  }

  @JSExport
  def takeDown(explainerId: String) = {
    dom.document.getElementById("publication-state-icon").asInstanceOf[Div].classList.add("state-indicator--loading")
    Model.takeDown(explainerId) onComplete {
      case Success(_) =>
        State.takenDown = true
        updateEmbedUrlAndStatusLabel(explainerId, TakenDown)
      case Failure(_) => g.console.error(s"Failed to take down explainer")
    }
  }

  @JSExport
  def setDisplayType(explainerId: String, displayType: String) = {
    updateFieldAndRefresh(explainerId, DisplayType, displayType, s"Failed to update displayType with string $displayType")
  }

  @JSExport
  def updateBodyContents(explainerId: String, bodyString: String) =
    updateFieldAndRefresh(explainerId, Body, bodyString, s"Failed to update body with string $bodyString")

  @JSExport
  def removeTagFromExplainer(explainerId: String, tagId: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate(RemoveTag, tagId)).map { explainer =>
      TagPickers.redisplayExplainerTagManagementAreas(explainer)
    }
  }

  @JSExport
  def presenceEnterDocument(explainerId: String) = {
    PresenceClient.enterDocument(explainerId)
  }

}