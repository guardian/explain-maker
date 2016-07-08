package presence

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

@ScalaJSDefined
class Person(val firstName: String, val lastName: String, val email: String) extends js.Object

@js.native
object PresenceGlobalScope extends js.GlobalScope {

  def presenceClient(endpoint: String, person: Person): PresenceClient = js.native
}

@js.native
trait PresenceClient extends js.Object {
  def startConnection(): Unit = js.native

  def on(presenceEvent: String, callback: js.Function1[js.Object, Unit]): Unit = js.native

  def subscribe(id: String): Unit = js.native

  def enter(articleId: String, articleLocation: String): Unit = js.native
}



object StateChange {

  case class Person(firstName: String, lastName: String, email: String) {
    val initials = Seq(firstName, lastName).flatMap(_.headOption).mkString
  }

  case class ClientId(connId: String, person: Person)

  case class State(clientId: ClientId, location: String)
}

case class StateChange(currentState: Seq[StateChange.State])