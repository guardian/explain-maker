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
import fr.hmil.roshttp.HttpRequest
import models.Tag
import org.scalajs.dom.raw.MouseEvent
import services.CAPIService
import upickle.Js
import upickle.default._

import scala.scalajs.js.JSON

object ExplainEditorJSDomBuilders {

  def statusBarText(explainer: CsAtom) = {
    val isPublished = explainer.contentChangeDetails.published.isDefined

    if(isPublished) {
      "published"
    } else {
      "draft"
    }
  }

  def redisplayExplainerTagManagementAreas(explainerId: String): Unit = {
    Model.extractExplainer(explainerId).map{ explainer =>

      dom.document.getElementById("explainer-editor__commissioning-desk-tags-wrapper").innerHTML = ""
      dom.document.getElementById("explainer-editor__commissioning-desk-tags-wrapper").appendChild(ExplainEditorJSDomBuilders.makeCommissioningDeskArea(explainer).render)

      dom.document.getElementById("explainer-editor__tags-wrapper").innerHTML = ""
      dom.document.getElementById("explainer-editor__tags-wrapper").appendChild(ExplainEditorJSDomBuilders.makeTagArea(explainer).render)

    }
  }

  def tagDeleteButton(explainer:CsAtom, tagId:String) = {
    val deleteButton = button(
      cls:="tag__delete",
      `type`:="button",
      data("explainer-id"):=explainer.id,
      data("tag-id"):=tagId
    )("Delete").render
    deleteButton.onclick = (x: Event) => {
      Model.removeTagFromExplainer(explainer.id, tagId).map { explainer =>
        redisplayExplainerTagManagementAreas(explainer.id)
      }
    }
    deleteButton
  }

  def explainerToDivTags(explainer:CsAtom, filterLambda: String => Boolean) = {
    explainer.data.tags match {
      case None => List()
      case Some(list) => list.filter( tagId => filterLambda(tagId) ).map(tagId => {
        div(cls:="tag")(
            tagId,tagDeleteButton(explainer,tagId)
          )
        }
      )
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

  def renderSuggestionSet(tags:List[Tag], explainerId:String, suggestionsDivIdentifier:String) = {
      g.jQuery(s"#$suggestionsDivIdentifier").empty()
      tags.foreach( tagObject => {
        val node = div(cls:="tag__result")(tagObject.webTitle).render
        node.onclick = (x: Event) => {
          Model.addTagToExplainer(explainerId, tagObject.id).map { explainer =>
            ExplainEditorJSDomBuilders.redisplayExplainerTagManagementAreas(explainer.id)
          }
        }
        dom.document.getElementById(suggestionsDivIdentifier).appendChild(node)
      })
  }

  def makeTagArea(explainer: CsAtom) = {
    val suggestionsDivIdentifier = "explainer-editor__tags__suggestions"
    val tagsSearchInput: TypedTag[Input] = input(
      id:="explainer-editor__tags__tag-search-input-field",
      cls:="form-field",
      placeholder:="tag search"
    )
    val tagsSearchInputTag = tagsSearchInput().render
    tagsSearchInputTag.oninput = (x: Event) => {
      val queryValue: String = g.readValueAtDiv("explainer-editor__tags__tag-search-input-field").asInstanceOf[String]

      CAPIService.capiTagRequest(Seq("type" -> "keyword", "q" -> queryValue))
        .map(renderSuggestionSet(_, explainer.id, suggestionsDivIdentifier))

    }
    renderTaggingArea(explainer, suggestionsDivIdentifier, "Tags", tagsSearchInputTag, { tagId => !tagId.startsWith("tracking") })
  }

  def makeCommissioningDeskArea(explainer: CsAtom) = {
    val suggestionsDivIdentifier = "explainer-editor__commissioning-desk-tags__suggestions"
    val tagsSearchInput = button(
      id:="explainer-editor__commissioning-desk-tags__tag-search-input-field",
      cls:="btn btn--secondary",
      `type`:="button"
    )("Add a commissioning desk").render
    tagsSearchInput.onclick = (x: Event) => {
      CAPIService.capiTagRequest(Seq("type" -> "tracking", "page-size" -> "200"))
        .map(renderSuggestionSet(_, explainer.id, suggestionsDivIdentifier)
      )
    }
    renderTaggingArea(explainer, suggestionsDivIdentifier, "Commissioning Desk", tagsSearchInput, { tagId => tagId.startsWith("tracking") })
  }

  def republishStatusBar(explainer: CsAtom) = {
    g.updateStatusBar(ExplainEditorJSDomBuilders.statusBarText(explainer))
  }

  def republishInteractiveURL(explainerId: String) = {
    val interactiveBaseUrl = g.CONFIG.INTERACTIVE_URL.toString
    val interactiveUrlText: String = s"$interactiveBaseUrl?id=${explainerId}"
    g.updateInteractiveURL(interactiveUrlText)
  }

  def SideBar(explainerId: String, explainer: CsAtom) = {

    val title: TypedTag[Input] = input(
      id:="explainer-editor__title-wrapper__input",
      cls:="form-field",
      placeholder:="Explainer Title",
      autofocus:=true
    )

    val titleTag = title(value := explainer.data.title).render
    titleTag.onchange = (x: Event) => {
      Model.updateFieldContent(explainerId, ExplainerUpdate("title", titleTag.value)).map(republishStatusBar)
    }

    val interactiveBaseUrl = g.CONFIG.INTERACTIVE_URL.toString

    val interactiveUrlText: String = explainer.contentChangeDetails.published match {
      case Some(thing) => s"$interactiveBaseUrl?id=$explainerId"
      case None => "Explainer has not been published yet"
    }

    val interactiveUrl: TypedTag[TextArea] = textarea(
      id:="interactive-url-text",
      cls:="form-field form-field--text-area text-monospaced",
      maxlength:=1800,
      readonly:="true"
    )

    val interactiveUrlTag = interactiveUrl(interactiveUrlText).render
    val interactiveURLWrapperDisplay = div(cls:="form-row")( div(cls:="form-label")("Interactive URL"), interactiveUrlTag )

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

    form()(
      div(cls:="form-row")(
          div(cls:="form-label")("Explainer Title"),
          titleTag
      ),
      interactiveURLWrapperDisplay,
      div(cls:="form-row")(
        div()(
          checkboxTag, " Expandable explainer"
        )
      ),
      div(cls:="explainer-editor__tag-management-wrapper")(
        div(
          id:="explainer-editor__commissioning-desk-tags-wrapper",
          cls:="form-row")(
          ExplainEditorJSDomBuilders.makeCommissioningDeskArea(explainer)
        ),
        div(
          id:="explainer-editor__tags-wrapper",
          cls:="form-row")(
          ExplainEditorJSDomBuilders.makeTagArea(explainer)
        )
      )
    )
  }

  def ExplainEditor(explainerId: String, explainer: CsAtom) = {

    val toolbarButtonTags = List(
      div(cls:="scribe-body-editor__toolbar-item", "data-command-name".attr:="bold")("Bold"),
      div(cls:="scribe-body-editor__toolbar-item", "data-command-name".attr:="italic")("Italic"),
      div(cls:="scribe-body-editor__toolbar-item", "data-command-name".attr:="linkPrompt")("Link"),
      div(cls:="scribe-body-editor__toolbar-item", "data-command-name".attr:="unLink")("Unlink"),
      div(cls:="scribe-body-editor__toolbar-item", "data-command-name".attr:="insertUnorderedList")("Unordered List")
    )

    val preventDefaultToolbarButtons = toolbarButtonTags.map(b => {
      val button = b.render
      button.onmousedown = (e: Event) => {
        e.preventDefault()
      }
      button
    })

    val toolbarTag: TypedTag[Div] = div(
      id:="scribe-toolbar",
      cls:="scribe-body-editor__toolbar"
    )(
      preventDefaultToolbarButtons
    )

    val scribeBodyEditorTextarea: TypedTag[Div] = div(
      id:="explainer-input-text-area",
      cls:="scribe-body-editor__textarea",
      maxlength:=1800,
      placeholder:="Explainer body text"
    )
    val bodyTag = scribeBodyEditorTextarea(raw(explainer.data.body)).render

    val editor = div(
      id:="explainer-editor",
      cls:="explainer")(
      form()(
        div(
          id:="explainer-editor__body-wrapper",
          cls:="explainer__body")(
          div(cls:="scribe-body-editor")(
            toolbarTag.render,
            bodyTag
          )
        )
      )
    )

    ExplainEditorPresenceHelpers.attachPresenceEventHandlerToElement(explainerId, editor.render)

  }

}
