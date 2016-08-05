import autowire.Macros.Check
import autowire._
import common.ExtAjax._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.{Element, Input, Option, TextArea}
import org.scalajs.dom.{Event, FocusEvent}
import presence.StateChange.State
import presence.{Person, PresenceGlobalScope, StateChange}
import rx._
import shared._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom._
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.section
import shared.models.{CsAtom, ExplainerUpdate}
import shared.models.CsAtom._
import upickle.Js
import upickle.default._

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

    val explainer = Var(CsAtom)

    def extractExplainer(id: String): Future[CsAtom] = {
      Ajaxer[ExplainerApi].load(id).call()
    }

    def updateFieldContent(id: String, explainer: ExplainerUpdate) = {
      Ajaxer[ExplainerApi].update(id, explainer.field, explainer.value).call()
    }

    def createNewExplainer() = {
      Ajaxer[ExplainerApi].create().call()
    }

    def publish(id: String): Future[CsAtom] = {
      Ajaxer[ExplainerApi].publish(id).call()
    }

    def addTagToExplainer(explainerId: String, tagId: String): Future[CsAtom] = {
      Ajaxer[ExplainerApi].addTagToExplainer(explainerId, tagId).call()
    }

    def removeTagFromExplainer(explainerId: String, tagId: String): Future[CsAtom] = {
      Ajaxer[ExplainerApi].removeTagFromExplainer(explainerId, tagId).call()
    }

  }

  def turnOnPresenceFor(explainerId: String ,presenceAreaName: String, field: Element) = {
    field.onfocus = (x: FocusEvent) => {
      presenceClient.enter("explain-" + explainerId, presenceAreaName)
    }

    val indicatorId = s"presence-indicator-${presenceAreaName}"
    val fieldPresenceIndicator = div(`class` := "field-presence-indicator", id := indicatorId)()

    presenceClient.on("visitor-list-updated", { data: js.Object =>
      val stateChange = upickle.default.read[StateChange](js.JSON.stringify(data))
      val statesOnThisArea: Seq[State] = stateChange.currentState.filter(_.location == presenceAreaName)
      //dom.document.getElementById(indicatorId).innerHTML = statesOnThisArea.map(_.clientId.person.initials).mkString(" ")
      ()
    })

    div(`class` := "presence-field-container") (
      fieldPresenceIndicator,
      div(cls:="form-group")(
        label(presenceAreaName.capitalize),
        field
      )
    )
  }

  def statusBarText(explainer: CsAtom) = {

    val isDraftState = (for {
      lastModifiedDate <- explainer.contentChangeDetails.lastModified
      publishedDate <- explainer.contentChangeDetails.published
    }yield {
      lastModifiedDate.date > publishedDate.date
    }).getOrElse(true)

    val status = if(isDraftState){
      "Draft State"
    }else{
      ""
    }
    status
  }

  def republishStatusBar(explainer: CsAtom) = {
    g.updateStatusBar(statusBarText(explainer))
  }

  def explainerToDivTags(explainer:CsAtom) = {
    explainer.data.tags match {
      case None => List()
      case Some(list) => list.map(tagId => div(cls:="explainer-editor__tags__existing-tags__tag")(
        span(
          cls:="explainer-editor__tags__existing-tags__tag-delete-icon",
          data("explainer-id"):=explainer.id,
          data("tag-id"):=tagId
        )("[x]")," ",tagId
      ))
    }
  }

  def makeTagArea(explainer: CsAtom) = {

    val tagsSearchInput: TypedTag[Input] = input(
      id:="explainer-editor__tags__tag-search-input-field",
      cls:="explainer-editor__tags__tag-search-input-field",
      placeholder:="tag search"
    )

    val tagsSearchInputTag = tagsSearchInput().render
    tagsSearchInputTag.oninput = (x: Event) => {

      val searchString: String = g.readValueAtDiv("explainer-editor__tags__tag-search-input-field").asInstanceOf[String]
      val xhr = new dom.XMLHttpRequest()
      xhr.open("GET", "https://content.guardianapis.com/tags?api-key="+g.CONFIG.CAPI_API_KEY+"&q="+g.encodeURIComponent(searchString))
      xhr.onload = (e: dom.Event) => {
        if (xhr.status == 200) {
          g.jQuery(".explainer-editor__tags__suggestions").empty()
          g.processCapiSearchResponse(js.JSON.parse(xhr.responseText).response)
        }
      }
      xhr.send()

    }

    div()(
      div(id:="explainer-editor__tags__input-field-wrapper")(
        div(cls:="form-group")(
          div("")(
            label(cls:="form-group")("Tags")
          ),
          div("")(
            tagsSearchInputTag
          )
        )
      ),
      div(cls:="explainer-editor__tags__suggestions", id:="explainer-editor__tags__suggestions")(""),
      div(cls:="explainer-editor__tags__existing-tags")(
        explainerToDivTags(explainer)
      )
    )

  }

  def ExplainEditor(explainerId: String, explainer: CsAtom) = {

    val title: TypedTag[Input] = input(
      id:="explainer-editor__title-wrapper__input",
      cls:="explainer-input-field",
      placeholder:="title",
      autofocus:=true
    )

    val titleTag = title(value := explainer.data.title).render
    titleTag.onchange = (x: Event) => {
      Model.updateFieldContent(explainerId, ExplainerUpdate("title", titleTag.value)).map(republishStatusBar)
    }

    val body: TypedTag[TextArea] = textarea(
      id:="explainer-input-text-area",
      cls:="explainer-editor__body-wrapper__input explainer-input-field",
      maxlength:=1800,
      placeholder:="body"
    )

    val bodyTag = body(explainer.data.body).render
    bodyTag.oninput = (x: Event) => {
      g.updateWordCountDisplay()
      g.updateWordCountWarningDisplay()
    }

    val publishButton = button(id:="explainer-editor__ops-wrapper__publish-button")("Publish").render
    publishButton.onclick = (x: Event) => {
      Model.publish(explainerId).map(republishStatusBar)
    }

    val checkboxClassName: String = "explainer-editor__displayType-checkbox"
    val checkbox: TypedTag[Input] = explainer.data.displayType match {
      case "Expandable" => {
        input(
          cls:=checkboxClassName,
          `type`:="checkbox",
          `checked`:= "checked"
        )
      }
      case "Flat" => {
        input(
          cls:=checkboxClassName,
          `type`:="checkbox"
        )
      }
    }
    val checkboxTag = checkbox.render
    checkboxTag.onchange = (x: Event) => {
      g.updateCheckboxState()
    }

    div(id:="explainer-editor")(
      div(id:="explainer-editor__ops-wrapper")(
        publishButton
      ),
      hr,
      div(cls:="explainer-editor__displayType-wrapper")(
        div(cls:="explainer-editor__displayType-inner")(
          checkboxTag, " Expandable explainer"
        )
      ),
      form()(
        div(id:="explainer-editor__title-wrapper")(
          turnOnPresenceFor(explainerId,"title",titleTag)
        ),
        div(id:="explainer-editor__tags-wrapper")(
          makeTagArea(explainer)
        ),
        div(id:="explainer-editor__body-wrapper")(
          turnOnPresenceFor(explainerId,"body",bodyTag)
        )
      )
    )

  }

  @JSExport
  def main(explainerId: String) = {
    val articleId = "explain-"+explainerId

    presenceClient.startConnection()

    presenceClient.on("connection.open", { data:js.Object =>
      presenceClient.subscribe(articleId)
    })

    Model.extractExplainer(explainerId).map { explainer: CsAtom =>
      dom.document.getElementById("content").appendChild(
        ExplainEditor(explainerId, explainer).render
      )
      republishStatusBar(explainer)
      g.updateWordCountDisplay()
      g.updateWordCountWarningDisplay()
      g.initiateEditor()
    }
  }

  @JSExport
  def generateExplainerTagManagement(explainerId: String): Future[String] = {
    Model.extractExplainer(explainerId).map( explainer => makeTagArea(explainer).render.outerHTML )
  }

  def redisplayExplainerTagManagement(explainerId: String) = {
    Model.extractExplainer(explainerId).map{ explainer =>
      dom.document.getElementById("explainer-editor__tags-wrapper").innerHTML = ""
      dom.document.getElementById("explainer-editor__tags-wrapper").appendChild(makeTagArea(explainer).render)
    }
  }

  @JSExport
  def CreateNewExplainer() = {
    Model.createNewExplainer().map{ explainer: CsAtom =>
      g.location.href = s"/explain/${explainer.id}"
    }
  }

  @JSExport
  def setDisplayType(explainerId: String, displayType: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate("displayType", displayType))
  }

  @JSExport
  def updateBodyContents(explainerId: String, bodyString: String) = {
    Model.updateFieldContent(explainerId, ExplainerUpdate("body", bodyString)).map(republishStatusBar)
  }

  @JSExport
  def addTagToExplainer(explainerId: String, tagId: String) = {
    Model.addTagToExplainer(explainerId, tagId).map( explainer =>
      redisplayExplainerTagManagement(explainer.id)
    )
  }

  @JSExport
  def removeTagFromExplainer(explainerId: String, tagId: String) = {
    Model.removeTagFromExplainer(explainerId, tagId).map( explainer =>
      redisplayExplainerTagManagement(explainer.id)
    )
  }

  @JSExport
  def addTagToSuggestionSet(explainerId: String, tagId: String) = {
    val node = div(cls:="explainer-editor__tag-suggestion__item")(tagId).render
    node.onclick = (x: Event) => {
      addTagToExplainer(explainerId, tagId)
    }
    dom.document.getElementById("explainer-editor__tags__suggestions").appendChild(node)
  }

}