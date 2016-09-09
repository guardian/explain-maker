package views

import api.Model
import components.statusbar.StatusBar
import components.{ScribeBodyEditor, Sidebar, TagPickers}
import org.scalajs.dom
import services.PresenceClient
import shared.models.UpdateField.{Body, DisplayType, RemoveTag, UpdateField}
import shared.models.{CsAtom, ExplainerUpdate}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport
import scala.util.{Failure, Success}
import scala.scalajs.js.{Function0, Object => JsObject}
import services.State
import shared.models.PublicationStatus.{Available, PublicationStatus, TakenDown}
import shared.util.SharedHelperFunctions


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

    Model.getExplainer(explainerId).map { explainer: CsAtom =>

      dom.document.getElementById("content").appendChild(
        ScribeBodyEditor.renderedBodyEditor(explainer)
      )

      getStatus(explainerId).map(s => {
        if (s == TakenDown) State.takenDown = true
        dom.document.getElementById("sidebar").appendChild(
          Sidebar.sidebar(explainer, s)
        )
        updateEmbedUrlAndStatusLabel(explainerId, s)
        })
      callback()
    }
  }

  def updateFieldAndRefresh(explainerId: String, updateField: UpdateField, updateValue: String, errorMessage: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate(Body, updateValue)) onComplete {
      case Success(e) => ExplainEditor.updateEmbedUrlAndStatusLabel(explainerId, SharedHelperFunctions.getExplainerStatusNoTakeDownCheck(e, State.takenDown))
      case Failure(_) => g.console.error(errorMessage)
    }
  }

  def getStatus(id: String, checkCapi: Boolean = true) = {
    Model.getExplainerStatus(id, checkCapi)
  }

  def updateEmbedUrlAndStatusLabel(id: String, status: PublicationStatus) = {
    StatusBar.updateStatusBar(status)
    Sidebar.republishembedURL(id, status)
  }

  @JSExport
  def publish(explainerId: String) = {
    Model.publish(explainerId) onComplete {
      case Success(_) =>
        State.takenDown = false
        updateEmbedUrlAndStatusLabel(explainerId, Available)
      case Failure(_) => g.console.error(s"Failed to publish explainer")
    }
  }

  @JSExport
  def takeDown(explainerId: String) = {
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