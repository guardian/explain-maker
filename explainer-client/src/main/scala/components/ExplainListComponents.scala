package components

import shared.models.CsTag

import scalatags.JsDom.all._

object ExplainListComponents {

  def tagsToSelectOptions(tags: Seq[CsTag], selectedTag: String) = {
    val opts = tags.map{ t =>
      val opt = option(value:=t.id)(t.webTitle).render
      opt.selected = t.id == selectedTag
      opt
    }
    opts.render
  }
}