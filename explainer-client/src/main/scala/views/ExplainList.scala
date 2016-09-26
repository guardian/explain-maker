package views

import api.Model
import components.ExplainListComponents
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Select
import shared.models.CsAtom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.URIUtils
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom._

@JSExport
object ExplainList {

  def replaceParams(key: String, value: String): Unit = {
    val keyEnc = URIUtils.encodeURI(key)
    val valueEnc = URIUtils.encodeURI(value)
    dom.document.location.search = s"$keyEnc=$valueEnc"
  }

  def insertParamAndRemovePageNumber(key: String, value: String): Unit = {
    val keyEnc = URIUtils.encodeURI(key)
    val valueEnc = URIUtils.encodeURI(value)
    dom.document.location.search = s"$keyEnc=$valueEnc"
    val parameters = dom.document.location.search.substring(1).split('&').flatMap(p => {
      if (p.startsWith(keyEnc)) {
        Some(s"$keyEnc=$valueEnc")
      } else if (p.startsWith("pageNumber")) {
        None
      } else Some(p)
    }).mkString("&")
    val newQueryString = if (!parameters.contains(keyEnc)) s"$parameters&$keyEnc=$valueEnc" else parameters

    dom.document.location.search = newQueryString
  }

  @JSExport
  def deskChanged() = {
    val dropdown: Select = dom.document.getElementById("desk-dropdown").asInstanceOf[Select]
    val selectedDesk = dropdown.value
    if (selectedDesk == "all-desks") {
      val urlWithoutQueryParameters = s"${dom.document.location.protocol}//${dom.document.location.host}${dom.document.location.pathname}"
      dom.document.location.assign(urlWithoutQueryParameters)
    } else {
      replaceParams("desk", selectedDesk)
    }
  }

  @JSExport
  def searchExplainers() = {
    val search: Select = dom.document.getElementById("explainer-search").asInstanceOf[Select]
    val searchQuery = search.value
    if(searchQuery != "") {
      insertParamAndRemovePageNumber("titleQuery", searchQuery)
    }
  }

  @JSExport
  def clearSearch() = {
    val search: Select = dom.document.getElementById("explainer-search").asInstanceOf[Select]
    search.value = ""
    insertParamAndRemovePageNumber("titleQuery", search.value)
  }

  @JSExport
  def createNewExplainer() = {
    Model.createNewExplainer().map{ explainer: CsAtom =>
      g.location.href = s"/explain/${explainer.id}"
    }
  }

  @JSExport
  def renderDeskList() = {
    val deskDropdown = dom.document.getElementById("desk-dropdown").asInstanceOf[Select]

    Model.getTrackingTags.map{ tags =>
      val opts = ExplainListComponents.tagsToSelectOptions(tags, deskDropdown.value)
      deskDropdown.appendChild(opts)
    }
  }
}
