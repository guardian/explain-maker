package components

import api.Model
import org.scalajs.dom._
import org.scalajs.dom.html._
import shared.models.{CsAtom, ExplainerUpdate}

import scala.scalajs.js.Dynamic._
import scala.util.{Failure, Success}
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.scalajs.dom
import services.State
import shared.models.PublicationStatus._
import shared.models.UpdateField.{Body, Title}
import shared.util.SharedHelperFunctions
import views.ExplainEditor

import scala.scalajs.js.Dynamic.{global => g}

object Sidebar {

  def title(explainer:CsAtom) = {

    import scala.concurrent.ExecutionContext.Implicits.global
    val titleInput  = input(
      id:="explainer-editor__title-wrapper__input",
      cls:="form-field",
      placeholder:="Explainer Title",
      autofocus:=true
    )(value := explainer.data.title).render

    titleInput.onchange = (x: Event) => {
      ExplainEditor.updateFieldAndRefresh(explainer.id, Title, titleInput.value, s"Failed to update title with string ${titleInput.value}")
    }
  }

  def embedUrlBox(embedUrlText: String) =
    div(cls:="form-row")(
      div(cls:="form-label")("Embed URL"),
      textarea(
        id:="interactive-url-text",
        cls:="form-field form-field--text-area text-monospaced",
        maxlength:=1800,
        readonly:="true"
      )(embedUrlText)
    )

  def embedUrlText(explainerId: String, status:PublicationStatus) = status match {
    case Available | UnlaunchedChanges => s"${g.CONFIG.INTERACTIVE_URL.toString}?id=$explainerId"
    case Draft => "Publish explainer to get embed URL."
    case TakenDown => "The explainer has been taken down. Republish to get URL."
  }

  def republishembedURL(explainerId: String, status: PublicationStatus = Available) = {
    dom.document.getElementById("interactive-url-text").textContent = embedUrlText(explainerId, status)
  }

  def displayTypeToggle(displayType: String, checkboxId: String) = {
    val checkboxClassName = "form-checkbox__input"
    val checkbox: TypedTag[Input] = displayType match {
      case "Expandable" =>
        input(
          cls:=checkboxClassName,
          `type`:="checkbox",
          checked:= true,
          id := checkboxId
        )
      case "Flat" =>
        input(
          cls:=checkboxClassName,
          `type`:="checkbox",
          id := checkboxId
        )
    }
    val checkboxTag = checkbox.render
    checkboxTag.onchange = (x: Event) => {
      g.updateCheckboxState()
    }
    checkboxTag
  }

  def sidebar(explainer: CsAtom, status: PublicationStatus) = {
    val checkboxId = "expandable"
    form()(
      div(cls:="form-row")(
        div(cls:="form-label")("Explainer Title"),
        title(explainer)
      ),
      embedUrlBox(embedUrlText(explainer.id, status)),
      div(cls:="form-row")(
        p(cls:="form-label")("Expandable Explainer"),
        div(cls:="form-checkbox")(
          displayTypeToggle(explainer.data.displayType, checkboxId),
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
          TagPickers.makeCommissioningDeskArea(explainer)
        ),
        div(
          id:="explainer-editor__tags-wrapper",
          cls:="form-row")(
          TagPickers.makeTagArea(explainer)
        )
      )
    ).render
  }

}