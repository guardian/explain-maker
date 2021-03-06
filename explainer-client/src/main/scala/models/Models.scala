package models

import org.scalajs.dom.html


case class StatusLabel(name: String, cssClass: String, warningMessage: Option[String] = None)
case class RenderedStatusLabel(status: html.Paragraph, warningMessage: html.Paragraph)

