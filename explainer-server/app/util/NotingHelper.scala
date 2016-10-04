package util

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConverters._


object NotingHelper {
  private val flagList = List("gu-flag", "gu-correct")
  // gu:note is deprecated and kept in case there is some
  // piece of content with such notes in it.
  private val noteList = List("gu:note", "gu-note", "gu-note-barrier")

  private val parse: String => Document = s => Jsoup.parseBodyFragment(s)
  val removeNotesAndUnwrapFlags: String => String = s => unwrapFlags(removeNotes(parse(s))).html()

  private def unwrapFlags(doc: Document): Element = {
    for(tag <- flagList) {
      val elements = doc.getElementsByTag(tag)
      for (element <- elements.asScala) { element.unwrap() }
    }
    doc.body
  }

  private def removeNotes(doc: Document): Document = {
    for (tag <- noteList) {
      val elements = doc.getElementsByTag(tag)
      for (element <- elements.asScala) { element.remove() }
    }

    doc
  }


}
