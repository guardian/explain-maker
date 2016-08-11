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
import services.CAPIService
import upickle.Js
import upickle.default._

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

  def addTagToExplainer(explainerId: String, tagId: String) = {
    Model.addTagToExplainer(explainerId, tagId).map { explainer =>
      ExplainEditorJSDomBuilders.redisplayExplainerTagManagementAreas(explainer.id)
    }
  }

  def addTagToSuggestionSet(explainerId: String, suggestionsDivIdentifier: String, tagId: String, userInterfaceDescription: String) = {
    val node = div(cls:="tag__result")(userInterfaceDescription).render
    node.onclick = (x: Event) => {
      addTagToExplainer(explainerId, tagId)
    }
    dom.document.getElementById(suggestionsDivIdentifier).appendChild(node)
  }

  def renderSuggestionSet(tags:List[Tag], explainerId:String, suggestionsDivIdentifier:String) = {
      g.jQuery(s"#$suggestionsDivIdentifier").empty()
      tags.foreach( tagObject =>
        addTagToSuggestionSet(explainerId, suggestionsDivIdentifier, tagObject.id, tagObject.webTitle)
      )
  }

  def makeTagArea(explainer: CsAtom) = {
    val suggestionsDivIdentifier = "explainer-editor__tags__suggestions"
    val tagsSearchInput: TypedTag[Input] = input(
      id:="explainer-editor__tags__tag-search-input-field",
      cls:="form-field form-field--btn-height",
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

    div(
      id:="explainer-editor",
      cls:="explainer container")(
      div(
        id:="explainer-editor__ops-wrapper",
        cls:="section clearfix"
      )(publishButton),
      div(cls:="clearfix")(
        div(cls:="right")(
          checkboxTag, " Expandable explainer"
        )
      ),
      form()(
        div(
          id:="explainer-editor__title-wrapper",
          cls:="explainer__title")(
          ExplainEditorPresenceHelpers.turnOnPresenceFor(explainerId,"title",titleTag)
        ),
        div(
          id:="explainer-editor__body-wrapper",
          cls:="explainer__body")(
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

