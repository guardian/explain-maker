package components.explaineditor

import api.Model
import components.statusbar.StatusBar
import models.{Tag => CapiTag}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html._
import services.{CAPIService, State}
import shared.models.PublicationStatus._
import shared.models.{CsAtom, ExplainerUpdate}
import shared.util.SharedHelperFunctions

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dynamic.{global => g}
import scala.util.{Failure, Success}
import scalatags.JsDom
import scalatags.JsDom._
import scalatags.JsDom.all._


object ExplainEditorJSDomBuilders {

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
    explainer.data.tags.map(_.filter(filterLambda).map(tagId => {
        div(cls:="tag")(
          tagId,tagDeleteButton(explainer,tagId)
        )
      })).getOrElse(List())
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

  def renderSuggestionSet(tags:List[CapiTag], explainerId:String, suggestionsDivIdentifier:String) = {
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
      placeholder:="Tag search"
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
    val tagsSearchInput: TypedTag[Input] = input(
      id:="explainer-editor__commissioning-desk-tags__tag-search-input-field",
      cls:="form-field",
      placeholder:="Commissioning desk"
    )
    val tagsSearchInputTag = tagsSearchInput().render
    tagsSearchInputTag.oninput = (x: Event) => {
      val queryValue: String = g.readValueAtDiv("explainer-editor__commissioning-desk-tags__tag-search-input-field").asInstanceOf[String]

      CAPIService.capiTagRequest(Seq("type" -> "tracking", "q" -> queryValue))
        .map(renderSuggestionSet(_, explainer.id, suggestionsDivIdentifier))

    }
    renderTaggingArea(explainer, suggestionsDivIdentifier, "Commissioning Desk", tagsSearchInputTag, { tagId => tagId.startsWith("tracking") })
  }

  def getInteractiveUrlText(id: String, status: PublicationStatus) = {
    status match {
      case Available | UnlaunchedChanges => s"${g.CONFIG.INTERACTIVE_URL.toString}?id=$id"
      case Draft => "Publish explainer to get embed URL."
      case TakenDown => "The explainer has beeen taken down. Republish to get URL."
    }
  }

  def republishembedURL(explainerId: String, status: PublicationStatus = Available) = {
    dom.document.getElementById("interactive-url-text").textContent = getInteractiveUrlText(explainerId, status)
  }

  def SideBar(explainerId: String, explainer: CsAtom, status: PublicationStatus) = {

    val title: TypedTag[Input] = input(
      id:="explainer-editor__title-wrapper__input",
      cls:="form-field",
      placeholder:="Explainer Title",
      autofocus:=true
    )

    val titleTag = title(value := explainer.data.title).render
    titleTag.onchange = (x: Event) => {
      Model.updateFieldContent(explainerId, ExplainerUpdate("title", titleTag.value)) onComplete {
        case Success(e) => ExplainEditorJS.updateEmbedUrlAndStatusLabel(explainerId, SharedHelperFunctions.getExplainerStatusNoTakeDownCheck(e, State.takenDown))
        case Failure(_) => g.console.error(s"Failed to update title with string ${titleTag.value}")
      }
    }

    val interactiveUrlText: String = getInteractiveUrlText(explainerId, status)

    val interactiveUrl: TypedTag[TextArea] = textarea(
      id:="interactive-url-text",
      cls:="form-field form-field--text-area text-monospaced",
      maxlength:=1800,
      readonly:="true"
    )

    val interactiveUrlTag = interactiveUrl(interactiveUrlText).render
    val interactiveURLWrapperDisplay = div(cls:="form-row")( div(cls:="form-label")("Interactive URL"), interactiveUrlTag )

    val checkboxClassName: String = "form-checkbox__input"
    val checkboxId: String = "expandable"
    val checkbox: TypedTag[Input] = explainer.data.displayType match {
      case "Expandable" => {
        input(
          cls:=checkboxClassName,
          `type`:="checkbox",
          checked:=true,
          id:=checkboxId
        )
      }
      case "Flat" => {
        input(
          cls:=checkboxClassName,
          `type`:="checkbox",
          id:=checkboxId
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
        p(cls:="form-label")("Expandable Explainer"),
        div(cls:="form-checkbox")(
          checkboxTag,
          label(
            cls:="form-checkbox__toggle",
            `for`:=checkboxId
          )
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
      div(cls:="scribe-body-editor__toolbar-item", "data-command-name".attr:="insertUnorderedList")("Bulleted list"),
      div(cls:="scribe-body-editor__toolbar-item", "data-command-name".attr:="insertOrderedList")("Numbered list")
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

    if (g.CONFIG.PRESENCE_ENABLED.toString == "true") {
      ExplainEditorPresenceHelpers.attachPresenceEventHandlerToElement(explainerId, editor.render)
    } else {
      editor.render
    }


  }

}