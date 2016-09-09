package util

import com.gu.contentatom.thrift.Atom
import services.CAPIService
import shared.models.PublicationStatus._

import scala.concurrent.Future


object HelperFunctions {
  def getCreatedByString(explainer: Atom) = {
    val firstNameLastName = for {
      created <- explainer.contentChangeDetails.created
      user <- created.user
      firstName <- user.firstName
      lastName <- user.lastName
    } yield {
      s"$firstName $lastName"
    }
    firstNameLastName.getOrElse("-")
  }

  def isPublished(explainer: Atom) = {
    if(explainer.contentChangeDetails.published.isDefined) "Published" else "Draft"
  }

  // works out current status of explainer. If checkCapiStatus is provided then a query to capi will not be made - instead
  // the value of checkCapiStatus will be used
  def getExplainerStatus(explainer: Atom, capiService: CAPIService, checkCapiStatus: Boolean, stage: String): Future[PublicationStatus] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val status = for {
      p <- explainer.contentChangeDetails.published
      lm <- explainer.contentChangeDetails.lastModified
    } yield {
      def capiStatusToExplainerStatus(inCapi: Boolean) = {
        if (inCapi) {
          if (p.date >= lm.date) Available
          else UnlaunchedChanges
        }
        else TakenDown
      }
      // explain maker doesn't publish to capi when running locally
      val explainerInCapi= if (stage != "DEV" && checkCapiStatus) {
        capiService.checkExplainerInCapi(explainer.id)
      } else Future(true)
      explainerInCapi.map(capiStatusToExplainerStatus)
    }
    status.getOrElse(Future(Draft))
  }
}