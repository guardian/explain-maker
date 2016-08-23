import org.scalajs.dom
import org.scalajs.dom.html.Element
import org.scalajs.dom.html.Div
import org.scalajs.dom.FocusEvent
import org.scalajs.dom.raw.MouseEvent
import presence.StateChange.State
import presence.{Person, PresenceGlobalScope, StateChange}

import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js
import scalatags.JsDom.all._
import upickle.default._

object ExplainEditorPresenceHelpers {
  val endpoint = "wss://presence.code.dev-gutools.co.uk/socket"
  var person = new Person(g.USER_FIRSTNAME.toString(),g.USER_LASTNAME.toString(),g.USER_EMAIL_ADDRESS.toString())
  var presenceClient = PresenceGlobalScope.presenceClient(endpoint, person)
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
      dom.document.getElementById("presence-names-display-wrapper").innerHTML = statesOnThisArea.map(_.clientId.person.initials).map( i => s"<span class=${ "presence-names-single" }>${i}<span>" ).mkString(" ")
      ()
    })
  }
}