package components.explainlist

import org.scalajs.dom
import org.scalajs.dom.html.Select

import scala.scalajs.js.URIUtils
import scala.scalajs.js.annotation.JSExport

@JSExport
object ExplainListJS {

  def replaceParams(key: String, value: String): Unit = {
    val keyEnc = URIUtils.encodeURI(key)
    val valueEnc = URIUtils.encodeURI(value)
    dom.document.location.search = s"$keyEnc=$valueEnc"
  }

  @JSExport
  def deskChanged() = {
    val dropdown: Select = dom.document.getElementById("desk-dropdown").asInstanceOf[Select]
    val selectedDesk = dropdown.value
    println(selectedDesk)
    if (selectedDesk == "all-desks") {
      val urlWithoutQueryParameters = s"${dom.document.location.protocol}//${dom.document.location.host}${dom.document.location.pathname}"
      dom.document.location.assign(urlWithoutQueryParameters)
    } else {
      replaceParams("desk", selectedDesk)
    }
  }
}
