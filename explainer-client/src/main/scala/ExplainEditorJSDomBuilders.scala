import org.scalajs.dom
import org.scalajs.dom.html._
import org.scalajs.dom.Event

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scalatags.JsDom._
import scalatags.JsDom.all._
import shared.models.{CsAtom, ExplainerUpdate}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalatags.JsDom

object ExplainEditorJSDomBuilders {

  def statusBarText(explainer: CsAtom) = {

    val isDraftState = (for {
      lastModifiedDate <- explainer.contentChangeDetails.lastModified
      publishedDate <- explainer.contentChangeDetails.published
    }yield {
      lastModifiedDate.date > publishedDate.date
    }).getOrElse(true)

    val status = if(isDraftState){
      "Draft"
    }else{
      ""
    }
    status
  }

  def explainerToDivTags(explainer:CsAtom, filterLambda: String => Boolean) = {
    explainer.data.tags match {
      case None => List()
      case Some(list) => list.filter( tagId => filterLambda(tagId) ).map(tagId => div(cls:="tag")(
        " ",tagId,button(
          cls:="tag__delete",
          `type`:="button",
          data("explainer-id"):=explainer.id,
          data("tag-id"):=tagId
        )("Delete")
      ))
    }
  }

  def renderTaggingArea(explainer:CsAtom, suggestionsDomId: String, fieldDescription:String, inputTag: JsDom.Modifier, explainerToDivFilterLambda: String => Boolean) ={
    div()(
      div(
        id:="explainer-editor__tags__input-field-wrapper",
        cls:="relative-parent")(
        div(cls:="form-group")(
          div("")(
            label(cls:="form-label")(fieldDescription)
          ),
          div("")(
            inputTag
          ),
          div(id:=suggestionsDomId, cls:="tag__suggestions")("")
        )
      ),
      div(cls:="tags")(
        explainerToDivTags(explainer, explainerToDivFilterLambda)
      )
    )
  }

  def capiXMLHttpRequest( queryFragment: String, divIdentifier: String, tagFieldToDisplay: String ) = {
    val xhr = new dom.XMLHttpRequest()
    xhr.open("GET", "https://content.guardianapis.com/tags?api-key="+g.CONFIG.CAPI_API_KEY+""+queryFragment+"")
    xhr.onload = (e: dom.Event) => {
      if (xhr.status == 200) {
        g.jQuery(".tag__suggestions").empty()
        g.processCapiSearchResponseTags(divIdentifier,js.JSON.parse(xhr.responseText).response,tagFieldToDisplay)
      }
    }
    xhr.send()
  }

  def makeTagArea(explainer: CsAtom) = {
    val tagsSearchInput: TypedTag[Input] = input(
      id:="explainer-editor__tags__tag-search-input-field",
      cls:="form-field form-field--btn-height",
      placeholder:="tag search"
    )
    val tagsSearchInputTag = tagsSearchInput().render
    tagsSearchInputTag.oninput = (x: Event) => {
      val searchString: String = g.readValueAtDiv("explainer-editor__tags__tag-search-input-field").asInstanceOf[String]
      capiXMLHttpRequest("&type=keyword&q="+g.encodeURIComponent(searchString), "explainer-editor__tags__suggestions", "id")
    }
    renderTaggingArea(explainer, "explainer-editor__tags__suggestions", "Tags", tagsSearchInputTag, { tagId => !tagId.startsWith("tracking") })
  }

  def makeCommissioningDeskArea(explainer: CsAtom) = {
    val tagsSearchInput = button(
      id:="explainer-editor__commissioning-desk-tags__tag-search-input-field",
      cls:="btn btn--secondary",
      `type`:="button"
    )("Add a commissioning desk").render
    tagsSearchInput.onclick = (x: Event) => {
      capiXMLHttpRequest("&type=tracking&page-size=200", "explainer-editor__commissioning-desk-tags__suggestions", "webTitle")
    }
    renderTaggingArea(explainer, "explainer-editor__commissioning-desk-tags__suggestions", "Commissioning Desk", tagsSearchInput, { tagId => tagId.startsWith("tracking") })
  }

  def republishStatusBar(explainer: CsAtom) = {
    g.updateStatusBar(ExplainEditorJSDomBuilders.statusBarText(explainer))
  }

  def ExplainEditor(explainerId: String, explainer: CsAtom) = {

    val title: TypedTag[Input] = input(
      id:="explainer-editor__title-wrapper__input",
      cls:="form-field form-field--large",
      placeholder:="title",
      autofocus:=true
    )

    val titleTag = title(value := explainer.data.title).render
    titleTag.onchange = (x: Event) => {
      Model.updateFieldContent(explainerId, ExplainerUpdate("title", titleTag.value)).map(republishStatusBar)
    }

    val body: TypedTag[TextArea] = textarea(
      id:="explainer-input-text-area",
      cls:="form-field",
      maxlength:=1800,
      placeholder:="body"
    )

    val bodyTag = body(explainer.data.body).render
    bodyTag.oninput = (x: Event) => {
      g.updateWordCountDisplay()
      g.updateWordCountWarningDisplay()
    }

    val publishButton = button(
      id:="explainer-editor__ops-wrapper__publish-button",
      cls:="btn right",
      `type`:="button"
    )("Publish").render
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
      div(
        id:="explainer-editor__ops-wrapper",
        cls:="section clearfix"
      )(publishButton),
      div(cls:="explainer-editor__displayType-wrapper")(
        div(cls:="explainer-editor__displayType-inner")(
          checkboxTag, " Expandable explainer"
        )
      ),
      form()(
        div(id:="explainer-editor__title-wrapper")(
          ExplainEditorPresenceHelpers.turnOnPresenceFor(explainerId,"title",titleTag)
        ),
        div(id:="explainer-editor__body-wrapper")(
          ExplainEditorPresenceHelpers.turnOnPresenceFor(explainerId,"body",bodyTag)
        ),
        div(cls:="explainer-editor__tag-management-wrapper")(
          div(
            id:="explainer-editor__commissioning-desk-tags-wrapper",
            cls:="column column--half")(
            ExplainEditorJSDomBuilders.makeCommissioningDeskArea(explainer)
          ),
          div(
            id:="explainer-editor__tags-wrapper",
            cls:="column column--half")(
            ExplainEditorJSDomBuilders.makeTagArea(explainer)
          )
        )
      )
    )

  }

}

