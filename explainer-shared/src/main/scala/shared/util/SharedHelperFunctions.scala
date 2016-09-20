package shared.util


import shared.models._

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

}
