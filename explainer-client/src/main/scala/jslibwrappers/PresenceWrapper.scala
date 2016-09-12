package jslibwrappers

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