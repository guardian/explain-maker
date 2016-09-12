package components

import org.scalajs.dom.html._
import org.scalajs.dom.{raw => _, _}
import services.PresenceClient
import shared.models.CsAtom

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import scala.scalajs.js.Dynamic.{global => g}

object ScribeBodyEditor {
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

  val toolbar: TypedTag[Div] = div(
    id:="scribe-toolbar",
    cls:="scribe-body-editor__toolbar")(
    preventDefaultToolbarButtons
  )

  def textArea(body:String): TypedTag[Div] = div(
    id:="explainer-input-text-area",
    cls:="scribe-body-editor__textarea",
    maxlength:=1800,
    placeholder:="Explainer body text"
  )(raw(body))

  def scribeBodyEditor(body:String) = div(
    id:="explainer-editor",
    cls:="explainer")(
    form()(
      div(
        id:="explainer-editor__body-wrapper",
        cls:="explainer__body")(
        div(cls:="scribe-body-editor")(
          toolbar,
          textArea(body)
        )
      )
    )
  )

  def renderedBodyEditor(explainer: CsAtom) = {
    val editor = scribeBodyEditor(explainer.data.body)
    if (g.CONFIG.PRESENCE_ENABLED.toString == "true") {
      PresenceClient.attachPresenceEventHandlerToElement(explainer.id, editor.render)
    } else {
      editor.render
    }
  }
}