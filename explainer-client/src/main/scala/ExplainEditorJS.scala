import autowire._
import common.ExtAjax._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.{Element, Input, TextArea}
import org.scalajs.dom.{Event, FocusEvent}
import presence.StateChange.State
import presence.{Person, PresenceGlobalScope, StateChange}
import rx._
import shared._
import upickle.Js
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom._
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.section

object Ajaxer extends autowire.Client[Js.Value, Reader, Writer]{
  override def doCall(req: Request): Future[Js.Value] = {
    Ajax.postAsJson(
      "/api/" + req.path.mkString("/"),
      upickle.json.write(Js.Obj(req.args.toSeq:_*))
    ).map(_.responseText)
      .map(upickle.json.read)
  }

  def read[Result: Reader](p: Js.Value) = readJs[Result](p)
  def write[Result: Writer](r: Result) = writeJs(r)
}

@JSExport
object ExplainEditorJS {

  val endpoint = "wss://presence.code.dev-gutools.co.uk/socket"
  val person = new Person("A","Nother","a.nother.@guardian.co.uk")
  val presenceClient = PresenceGlobalScope.presenceClient(endpoint, person)

  object Model {
    import org.scalajs.jquery.{jQuery => $}

    val explainer = Var(ExplainerItem)

    def extractExplainer(id: String): Future[ExplainerItem] = {
      Ajaxer[ExplainerApi].load(id).call()
    }

    def updateFieldContent(id: String, explainer: ExplainerUpdate) = {
      Ajaxer[ExplainerApi].update(id, explainer.field, explainer.value).call()
    }

    def createNewExplainer() = {
      Ajaxer[ExplainerApi].create().call()
    }

    def publish(id: String): Future[ExplainerItem] = {
      Ajaxer[ExplainerApi].publish(id).call()
    }

  }

  def turnOnPresenceFor(explainerId: String ,area: String, field: Element) = {
    field.onfocus = (x: FocusEvent) => {
      presenceClient.enter("explain-" + explainerId, area)
    }

    val indicatorId = s"presence-indicator-$area"
    val fieldPresenceIndicator = div(`class` := "field-presence-indicator", id := indicatorId)()

    presenceClient.on("visitor-list-updated", { data: js.Object =>
      val stateChange = upickle.default.read[StateChange](js.JSON.stringify(data))
      val statesOnThisArea: Seq[State] = stateChange.currentState.filter(_.location == area)
      dom.document.getElementById(indicatorId).innerHTML = statesOnThisArea.map(_.clientId.person.initials).mkString(" ")
      ()
    })

    div(`class` := "presence-field-container") (
      fieldPresenceIndicator,
      div(cls:="form-group")(
        label(area),
        field
      )
    )
  }

  def statusBar(explainer: ExplainerItem) = {
    val isDraftState =  !explainer.live.isDefined || explainer.draft.title!=explainer.live.get.title || explainer.draft.body!=explainer.live.get.body
    val status = if(isDraftState){
      "Draft state: click on [Publish] to publish."
    }else{
      ""
    }
    div(id:="explainer-editor__ops-wrapper__status-bar-wrapper__status-bar",cls:="red")(status)
  }

  def republishStatusBar(explainer: ExplainerItem) = {
    dom.document.getElementById("explainer-editor__ops-wrapper__status-bar-wrapper").innerHTML = statusBar(explainer).render.innerHTML
  }

  def ExplainEditor(explainerId: String, explainer: ExplainerItem) = {

    val title: TypedTag[Input] = input(
      id:="explainer-editor__title-wrapper__input",
      cls:="explainer-input-field",
      placeholder:="title",
      autofocus:=true
    )

    val titleTag = title(value := explainer.draft.title).render
    titleTag.onchange = (x: Event) => {
      Model.updateFieldContent(explainerId, ExplainerUpdate("title", titleTag.value)).map(republishStatusBar)
    }

    val body: TypedTag[TextArea] = textarea(
      id:="explainer-editor__body-wrapper__input",
      cls:="explainer-input-field",
      maxlength:=1800,
      placeholder:="body"
    )

    val bodyTag = body(explainer.draft.body).render
    bodyTag.onchange = (x: Event) => {
      Model.updateFieldContent(explainerId, ExplainerUpdate("body", bodyTag.value)).map(republishStatusBar)
    }

    val publishButton = button(id:="explainer-editor__ops-wrapper__publish-button")("Publish").render
    publishButton.onclick = (x: Event) => {
      Model.publish(explainerId).map(republishStatusBar)
    }

    div(id:="explainer-editor")(
      div(id:="explainer-editor__ops-wrapper")(
        publishButton,
        div(id:="explainer-editor__ops-wrapper__status-bar-wrapper")(
          statusBar(explainer)
        )
      ),
      hr,
      form()(
        div(id:="explainer-editor__title-wrapper")(
          turnOnPresenceFor(explainerId,"title",titleTag)
        ),
        div(id:="explainer-editor__body-wrapper")(
          turnOnPresenceFor(explainerId,"body",bodyTag)
        )
      )
    )

  }

  @JSExport
  def main(explainerId: String) = {
    g.console.log(person)
    val articleId = "explain-"+explainerId

    presenceClient.startConnection()

    presenceClient.on("connection.open", { data:js.Object =>
      presenceClient.subscribe(articleId)
    })

    Model.extractExplainer(explainerId).map { explainer: ExplainerItem =>
      dom.document.getElementById("content").appendChild(
        ExplainEditor(explainerId, explainer).render
      )
    }
  }

  @JSExport
  def CreateNewExplainer() = {
    Model.createNewExplainer().map{ explainer: ExplainerItem =>
      g.location.href = s"/explain/${explainer.id}"
    }
  }

}