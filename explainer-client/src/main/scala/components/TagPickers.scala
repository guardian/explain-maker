package components

import api.Model
import models.Tag
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html._
import services.CAPIClient
import shared.models.{CsAtom, ExplainerUpdate}
import models.{Tag => CapiTag}
import shared.models.UpdateField.{AddTag, RemoveTag}

import scala.scalajs.js.Dynamic._
import scalatags.JsDom
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import scala.scalajs.js.Dynamic.{global => g}
import scala.concurrent.ExecutionContext.Implicits.global

object TagPickers {

  def redisplayExplainerTagManagementAreas(explainer: CsAtom): Unit = {
    dom.document.getElementById("explainer-editor__commissioning-desk-tags-wrapper").innerHTML = ""
    dom.document.getElementById("explainer-editor__commissioning-desk-tags-wrapper").appendChild(makeCommissioningDeskArea(explainer).render)

    dom.document.getElementById("explainer-editor__tags-wrapper").innerHTML = ""
    dom.document.getElementById("explainer-editor__tags-wrapper").appendChild(makeTagArea(explainer).render)
  }

  def tagDeleteButton(explainer:CsAtom, tagId:String) = {
    val deleteButton = button(
      cls:="tag__delete",
      `type`:="button",
      data("explainer-id"):=explainer.id,
      data("tag-id"):=tagId
    )("Delete").render
    deleteButton.onclick = (x: Event) => {
      Model.updateFieldContent(explainer.id, ExplainerUpdate(RemoveTag, tagId)).map { explainer =>
        redisplayExplainerTagManagementAreas(explainer)
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
      val node = button(cls:="tag__result")(tagObject.webTitle).render
      node.onmousedown = (x: Event) => {
        println("CLICKED")
        Model.updateFieldContent(explainerId, ExplainerUpdate(AddTag, tagObject.id)).map { explainer =>
          redisplayExplainerTagManagementAreas(explainer)
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
      val queryValue: String = dom.document.getElementById("explainer-editor__tags__tag-search-input-field").asInstanceOf[Input].value

      CAPIClient.capiTagRequest(Seq("type" -> "keyword", "q" -> queryValue))
        .map(renderSuggestionSet(_, explainer.id, suggestionsDivIdentifier))

    }

    dom.window.onmouseup = (x: Event) => {
      dom.document.getElementById(suggestionsDivIdentifier).innerHTML= ""
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
      val queryValue: String = dom.document.getElementById("explainer-editor__commissioning-desk-tags__tag-search-input-field").asInstanceOf[Input].value

      CAPIClient.capiTagRequest(Seq("type" -> "tracking", "q" -> queryValue))
        .map(renderSuggestionSet(_, explainer.id, suggestionsDivIdentifier))

    }
    dom.window.onmouseup = (x: Event) => {
      dom.document.getElementById(suggestionsDivIdentifier).innerHTML = ""
    }
    renderTaggingArea(explainer, suggestionsDivIdentifier, "Commissioning Desk", tagsSearchInputTag, { tagId => tagId.startsWith("tracking") })
  }

}