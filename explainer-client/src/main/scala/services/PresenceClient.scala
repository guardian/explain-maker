package services

import jslibwrappers.{Person, PresenceGlobalScope}
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.Element
import scalatags.JsDom.all._

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

object PresenceClient {
  val endpoint = s"wss://presence.${g.CONFIG.PRESENCE_ENDPOINT_URL.toString}/socket"
  val person = new Person(g.CONFIG.USER_FIRSTNAME.toString, g.CONFIG.USER_LASTNAME.toString, g.CONFIG.USER_EMAIL_ADDRESS.toString)
  val presenceClient = PresenceGlobalScope.presenceClient(endpoint, person)
  def attachPresenceEventHandlerToElement(explainerId: String, element: Element) = {
    element.onmouseover = (x: MouseEvent) => {
      presenceClient.enter(s"explain-$explainerId", "document")
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
      if (stateChange.currentState.length > 1) {
        dom.document.getElementById("presence-warning-message").classList.remove("visually-hidden")
      } else {
        dom.document.getElementById("presence-warning-message").classList.add("visually-hidden")
      }
      val statesOnThisArea: Seq[StateChange.State] = stateChange.currentState.filter(_.location == "document")
      dom.document.getElementById("presence-names-display-wrapper").innerHTML = statesOnThisArea.map(_.clientId.person.initials).map( i => span(cls:="presence-names-single")(i) ).mkString(" ")
      ()
    })
  }
}

object StateChange {

  case class Person(firstName: String, lastName: String, email: String) {
    val initials = Seq(firstName, lastName).flatMap(_.headOption).mkString
  }

  case class ClientId(connId: String, person: Person)

  case class State(clientId: ClientId, location: String)
}

case class StateChange(currentState: Seq[StateChange.State])