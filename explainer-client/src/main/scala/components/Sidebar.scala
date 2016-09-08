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
import shared.models.PublicationStatus._
import shared.models.UpdateField.Title
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
      Model.updateFieldContent(explainer.id, ExplainerUpdate(Title, titleInput.value)) onComplete {
        case Success(_) => ExplainEditor.updateEmbedUrlAndStatusLabel(explainer.id, checkCapi=false)
        case Failure(_) => g.console.error(s"Failed to update title with string ${titleInput.value}")
      }
    }
  }

  val embedUrlBox =
    div(cls:="form-row")(
      div(cls:="form-label")("Embed URL"),
      textarea(
        id:="interactive-url-text",
        cls:="form-field form-field--text-area text-monospaced",
        maxlength:=1800,
        readonly:="true"
      )("Unable to determine interactive url")
    )

  def republishembedURL(explainerId: String, status: PublicationStatus = Available) = {
    val interactiveUrlText = status match {
      case Available | UnlaunchedChanges => s"${g.CONFIG.INTERACTIVE_URL.toString}?id=$explainerId"
      case Draft => "Publish explainer to get embed URL."
      case TakenDown => "The explainer has been taken down. Republish to get URL."
    }
    dom.document.getElementById("interactive-url-text").textContent = interactiveUrlText
  }

  def displayTypeToggle(displayType: String) = {
    val checkboxClassName = "explainer-editor__displayType-checkbox"
    val checkbox: TypedTag[Input] = displayType match {
      case "Expandable" =>
        input(
          cls:=checkboxClassName,
          `type`:="checkbox",
          `checked`:= "checked"
        )
      case "Flat" =>
        input(
          cls:=checkboxClassName,
          `type`:="checkbox"
        )
    }
    val checkboxTag = checkbox.render
    checkboxTag.onchange = (x: Event) => {
      g.updateCheckboxState()
    }
  }

  def sidebar(explainer: CsAtom) = {
    form()(
      div(cls:="form-row")(
        div(cls:="form-label")("Explainer Title"),
        title(explainer)
      ),
      embedUrlBox,
      div(cls:="form-row")(
        div()(
          displayTypeToggle(explainer.data.displayType), " Expandable explainer"
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