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

object ExplainEditorJSDomBuilders {
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
        ExplainEditorJSDomBuilders.explainerToDivTags(explainer)
      )
    )

  }

  def republishStatusBar(explainer: CsAtom) = {
    g.updateStatusBar(ExplainEditorJSDomBuilders.statusBarText(explainer))
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
          ExplainEditorPresenceHelpers.turnOnPresenceFor(explainerId,"title",titleTag)
        ),
        div(id:="explainer-editor__tags-wrapper")(
          ExplainEditorJSDomBuilders.makeTagArea(explainer)
        ),
        div(id:="explainer-editor__body-wrapper")(
          ExplainEditorPresenceHelpers.turnOnPresenceFor(explainerId,"body",bodyTag)
        )
      )
    )

  }

}

