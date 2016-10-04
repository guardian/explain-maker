package shared.util


import shared.models._
import com.gu.contentatom.thrift.AtomDataAliases

object SharedHelperFunctions {
  def getExplainerStatusNoTakeDownCheck(explainer: CsAtom, isTakenDown: Boolean): PublicationStatus = {
    if (isTakenDown) TakenDown else {
      val status = for {
        p <- explainer.contentChangeDetails.published
        lm <- explainer.contentChangeDetails.lastModified
      } yield {
        if (p.date >= lm.date) Available
        else UnlaunchedChanges
      }
      status.getOrElse(Draft)
    }
  }

  def generateDefaultHtml(atomData: AtomDataAliases.ExplainerAlias): String = {
    s"""<figure class=\"element element-explainer element-atom\" data-display-type=\"${atomData.displayType.toString.toLowerCase}\">
       |  ${if (!atomData.title.isEmpty) s"<h1>${atomData.title}</h1>" else ""}
       |  ${if (!atomData.body.isEmpty) s"<div>${atomData.body}</div>" else ""}
       |</figure>
     """.stripMargin
  }

}

