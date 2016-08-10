import org.scalajs.dom.html.{Element}
import org.scalajs.dom.{FocusEvent}
import presence.StateChange.State
import presence.{Person, PresenceGlobalScope, StateChange}

import scala.scalajs.js
import scalatags.JsDom.all._
import upickle.default._

object ExplainEditorPresenceHelpers {
  val endpoint = "wss://presence.code.dev-gutools.co.uk/socket"
  val person = new Person("A","Nother","a.nother.@guardian.co.uk")
//  val presenceClient = PresenceGlobalScope.presenceClient(endpoint, person)
  def turnOnPresenceFor(explainerId: String ,presenceAreaName: String, field: Element) = {
    field.onfocus = (x: FocusEvent) => {
//      presenceClient.enter("explain-" + explainerId, presenceAreaName)
    }

    val indicatorId = s"presence-indicator-${presenceAreaName}"
    val fieldPresenceIndicator = div(`class` := "field-presence-indicator", id := indicatorId)()

//    presenceClient.on("visitor-list-updated", { data: js.Object =>
//      val stateChange = upickle.default.read[StateChange](js.JSON.stringify(data))
//      val statesOnThisArea: Seq[State] = stateChange.currentState.filter(_.location == presenceAreaName)
//      //dom.document.getElementById(indicatorId).innerHTML = statesOnThisArea.map(_.clientId.person.initials).mkString(" ")
//      ()
//    })

    div(`class` := "presence-field-container") (
      fieldPresenceIndicator,
      div(cls:="form-group")(
        label(cls:="form-label")(presenceAreaName.capitalize),
        field
      )
    )
  }
}