package components.explaineditor

import org.scalajs.dom.html.Element
import presence.Person
import presence._
import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom._
import presence.StateChange.State

import scala.scalajs.js.Dynamic.{global => g}


object ExplainEditorPresenceHelpers {
  val endpoint = g.CONFIG.PRESENCE_ENDPOINT_URL.toString
  val person = new Person(g.CONFIG.USER_FIRSTNAME.toString, g.CONFIG.USER_LASTNAME.toString, g.CONFIG.USER_EMAIL_ADDRESS.toString)
  val presenceClient = PresenceGlobalScope.presenceClient(endpoint, person)
  def attachPresenceEventHandlerToElement(explainerId: String, element: Element) = {
    element.onmouseover = (x: MouseEvent) => {
      presenceClient.enter("explain-" + explainerId, "document")
    }
    enterDocument(explainerId)
    activatePresenceHandler()
    element
  }
  def enterDocument(explainerId: String) = {
    presenceClient.enter("explain-" + explainerId, "document")
  }
  def activatePresenceHandler() = {
    presenceClient.on("visitor-list-updated", { data: js.Object =>
      val stateChange = upickle.default.read[StateChange](js.JSON.stringify(data))
      val statesOnThisArea: Seq[State] = stateChange.currentState.filter(_.location == "document")
      dom.document.getElementById("presence-names-display-wrapper").innerHTML = statesOnThisArea.map(_.clientId.person.initials).map( i => s"<span class=${ "presence-names-single" }>${i}</span>" ).mkString(" ")
      ()
    })
  }
}