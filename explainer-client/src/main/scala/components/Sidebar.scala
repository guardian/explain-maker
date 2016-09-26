package components

import api.Model
import org.scalajs.dom._
import org.scalajs.dom.html._
import shared.models._

import scala.scalajs.js.Dynamic._
import scala.util.{Failure, Success}
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.scalajs.dom
import services.State
import shared.models.UpdateField.{Body, Title}
import shared.models.WorkflowStatus._
import shared.util.SharedHelperFunctions
import views.ExplainEditor

import scala.scalajs.js.Dynamic.{global => g}

object Sidebar {

  val embedUrlString = s"${g.CONFIG.INTERACTIVE_URL.toString}?id=${g.CONFIG.EXPLAINER_IDENTIFIER.toString}"

  def title(explainer:CsAtom) = {

    import scala.concurrent.ExecutionContext.Implicits.global
    val titleInput  = input(
      id:="explainer-editor__title-wrapper__input",
      cls:="form-field",
      placeholder:="Text Atom Title",
      autofocus:=true
    )(value := explainer.data.title).render

    titleInput.onchange = (x: Event) => {
      ExplainEditor.updateFieldAndRefresh(explainer.id, Title, titleInput.value, s"Failed to update title with string ${titleInput.value}")
    }
    titleInput
  }

  def embedUrlBox(embedUrlText: String) =
    div(cls:="form-row")(
      div(cls:="form-label")("Embed URL ", span(id:="embed-preview-link", cls:="preview-link")(a(href:=embedUrlString, target:="_blank")("Preview"))),
      textarea(
        id:="interactive-url-text",
        cls:="form-field form-field--text-area text-monospaced",
        maxlength:=1800,
        readonly:="true"
      )(embedUrlText)
    )

  def embedUrlText(explainerId: String, status:PublicationStatus) = status match {
    case Available | UnlaunchedChanges => embedUrlString
    case Draft => "Publish text atom to get embed URL."
    case TakenDown => "The text atom has been taken down. Republish to get URL."
  }

  def statusToOption(status: WorkflowStatus, currentStatus: WorkflowStatus) = {
    val opt = option(value:= status.toString)(status.toString).render
    opt.selected = status == currentStatus
    opt
  }

  def wfStatusOptions(currentStatus: WorkflowStatus) = {
    List(statusToOption(Writers, currentStatus), statusToOption(Desk, currentStatus), statusToOption(Subs, currentStatus), statusToOption(Live, currentStatus))
  }

  def workflowStatusDropdown(id: String, currentStatus: WorkflowStatus) = {
    val dropdown = select(cls := "form-field form-field--select")(wfStatusOptions(currentStatus)).render
    dropdown.onchange = (x:Event) => {
      Model.setWorkflowData(WorkflowData(id, WorkflowStatus(dropdown.value)))
    }
    div(cls:="form-row")(
      div(cls:="form-label")("Status"),
      dropdown
    )
  }

  def updatePreviewLink(status: PublicationStatus, embedUrl: String) ={
    val interactivePreviewLink = dom.document.getElementById("embed-preview-link")
    status match {

      case Available | UnlaunchedChanges => {
        interactivePreviewLink.asInstanceOf[Span].classList.remove("preview-link--hidden")
        interactivePreviewLink.asInstanceOf[Span].classList.add("preview-link")
      }
      case _ => {
        interactivePreviewLink.asInstanceOf[Span].classList.remove("preview-link")
        interactivePreviewLink.asInstanceOf[Span].classList.add("preview-link--hidden")
      }
    }
  }

  def republishembedURL(explainerId: String, status: PublicationStatus = Available) = {
    val urlText = embedUrlText(explainerId, status)
    updatePreviewLink(status, urlText)
    dom.document.getElementById("interactive-url-text").textContent = urlText
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

  def sidebar(explainer: CsAtom, status: PublicationStatus, workflowStatus: WorkflowStatus) = {
    val checkboxId = "expandable"
    form()(
      div(cls:="form-row")(
        div(cls:="form-label")("Text Atom Title"),
        title(explainer)
      ),
      embedUrlBox(embedUrlText(explainer.id, status)),
      div(cls:="form-row")(
        p(cls:="form-label")("Expandable Text Atom"),
        div(cls:="form-checkbox")(
          displayTypeToggle(explainer.data.displayType, checkboxId),
          label(
            cls:="form-checkbox__toggle",
            `for`:=checkboxId
          )
        )
      ),
      workflowStatusDropdown(explainer.id, workflowStatus),
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