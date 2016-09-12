package components.statusbar

import models.{RenderedStatusLabel, StatusLabel}
import org.scalajs.dom
import shared.models.PublicationStatus._

import scalatags.JsDom.all._
import org.scalajs.dom.html.Button

object StatusBar {

  val available = StatusLabel("Available", "label--success")
  val draft = StatusLabel("Draft", "label--warning")
  val unlaunchedChanges = StatusLabel("Available", "label--success", Some("Unlaunched changes"))
  val unknown = StatusLabel("Unknown", "label--success")
  val takenDown = StatusLabel("Taken Down", "label--error")

  def renderStatusLabel(l: StatusLabel) = {

    val statusLabel = p(cls := s"label content-status ${l.cssClass}")(l.name).render
    val warningMessage = l.warningMessage.fold(p())(p(cls := s"label label--warning")(_)).render
    RenderedStatusLabel(statusLabel, warningMessage)
  }

  def updateStatusBar(status: PublicationStatus): Unit = {
    def updateStatusBarDom(label: StatusLabel) = {

      dom.document.getElementById("explainer-publication-status").innerHTML = ""
      val renderedStatusLabel = renderStatusLabel(label)
      dom.document.getElementById("explainer-publication-status").appendChild(renderedStatusLabel.status)
      dom.document.getElementById("explainer-publication-status").appendChild(renderedStatusLabel.warningMessage)
    }

    def setTakeDownButtonDisabled(disabled: Boolean) = {
      dom.document.getElementById("toolbar-takedown-button").asInstanceOf[Button].disabled = disabled
    }

    status match {
      case Available =>
        setTakeDownButtonDisabled(false)
        updateStatusBarDom(available)
      case UnlaunchedChanges =>
        setTakeDownButtonDisabled(false)
        updateStatusBarDom(unlaunchedChanges)
      case Draft =>
        updateStatusBarDom(draft)
        setTakeDownButtonDisabled(true)
      case TakenDown =>
        setTakeDownButtonDisabled(true)
        updateStatusBarDom(takenDown)
    }

  }
}