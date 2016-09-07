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

  def getStatus(id: String, checkCapi: Boolean) = {
    Model.getExplainerStatus(id, checkCapi)
  }

  def updateEmbedUrlAndStatusLabel(id: String, checkCapi: Boolean = true) = {
    val status = getStatus(id, checkCapi)
    status.map(s => {
      StatusBar.updateStatusBar(s)
      ExplainEditorJSDomBuilders.republishembedURL(id, s)
    })
  }

  @JSExport
  def main(explainerId: String, callback: js.Function0[Unit]) = {

    val articleId = "explain-"+explainerId

    if(g.CONFIG.PRESENCE_ENABLED.toString == "true") {
      ExplainEditorPresenceHelpers.presenceClient.startConnection()
      ExplainEditorPresenceHelpers.presenceClient.on("connection.open", { data:js.Object =>
        ExplainEditorPresenceHelpers.presenceClient.subscribe(articleId)
      })
    }


    Model.extractExplainer(explainerId).map { explainer: CsAtom =>

      dom.document.getElementById("content").appendChild(
        ExplainEditorJSDomBuilders.ExplainEditor(explainerId, explainer)
      )

      dom.document.getElementById("sidebar").appendChild(
        ExplainEditorJSDomBuilders.SideBar(explainerId, explainer).render
      )
      updateEmbedUrlAndStatusLabel(explainerId)
      callback()

    }
  }

  @JSExport
  def generateExplainerTagManagement(explainerId: String): Future[String] = {
    Model.extractExplainer(explainerId).map( explainer => ExplainEditorJSDomBuilders.makeTagArea(explainer).render.outerHTML )
  }

  @JSExport
  def createNewExplainer() = {
    Model.createNewExplainer().map{ explainer: CsAtom =>
      g.location.href = s"/explain/${explainer.id}"
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
      ExplainEditorJSDomBuilders.redisplayExplainerTagManagementAreas(explainer.id)
    }
  }

  @JSExport
  def presenceEnterDocument(explainerId: String) = {
    ExplainEditorPresenceHelpers.enterDocument(explainerId)
  }

}