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

  @JSExport
  def main() = {
    renderDeskList()
    updatePageNumberButtons(g.PAGE_NUMBER.toString.toInt, g.MAX_PAGE_NUMBER.toString.toInt)
  }

  def insertParamAndRemovePageNumber(key: String, value: String): Unit = {
    val keyEnc = URIUtils.encodeURI(key)
    val valueEnc = URIUtils.encodeURI(value)
    val newParameter = s"$keyEnc=$valueEnc"

    val parameters = dom.document.location.search.substring(1).split('&').flatMap(p => {
      if (p.startsWith(keyEnc)) {
        Some(newParameter)
      } else if (p.startsWith("pageNumber")) {
        None
      } else Some(p)
    }).mkString("&")

    val newQueryString = if (!parameters.contains(keyEnc)) {

      if (parameters.length > 0 ) {
        s"$parameters&$newParameter"
      } else {
        newParameter
      }

    } else parameters

    dom.document.location.search = newQueryString
  }

  def updatePageNumberButtons(pageNo: Int, maxPageNo: Int): Unit = {
    if (pageNo > 1) {
      dom.document.getElementById("previous-page-link").classList.remove("visually-hidden")
    } else {
      dom.document.getElementById("previous-page-link").classList.add("visually-hidden")
    }
    if (pageNo < maxPageNo) {
      dom.document.getElementById("next-page-link").classList.remove("visually-hidden")
    } else {
      dom.document.getElementById("next-page-link").classList.add("visually-hidden")
    }
  }

  def updateOrInsertParam(paramName: String, newValue: String) = {
    val newValueEnc = URIUtils.encodeURI(newValue)
    val queryString = dom.document.location.search.substring(1)
    val newQueryString = if (queryString.contains(paramName)) {
      queryString.split('&').map { p =>
        if (p.startsWith(paramName)) {
          s"$paramName=${URIUtils.encodeURI(newValue)}"
        } else p
      }.mkString("&")
    } else if (queryString.length > 0) {
      s"$queryString&$paramName=$newValueEnc"
    } else {
      s"$paramName=$newValueEnc"
    }
    dom.document.location.search = newQueryString
  }

  @JSExport
  def updatePageNumber(no: Int) = {
    updateOrInsertParam("pageNumber", no.toString)
  }

  @JSExport
  def deskChanged() = {
    val dropdown: Select = dom.document.getElementById("desk-dropdown").asInstanceOf[Select]
    val selectedDesk = dropdown.value
    if (selectedDesk == "all-desks") {
      val urlWithoutQueryParameters = s"${dom.document.location.protocol}//${dom.document.location.host}${dom.document.location.pathname}"
      dom.document.location.assign(urlWithoutQueryParameters)
    } else {
      dom.document.location.search = s"desk=${URIUtils.encodeURI(selectedDesk)}"
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
