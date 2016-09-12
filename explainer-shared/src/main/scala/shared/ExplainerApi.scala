package shared

import shared.models.{CsAtom, ExplainerUpdate}
import shared.models.PublicationStatus.PublicationStatus

import scala.concurrent.Future

// shared interface
trait ExplainerApi {

  def create(): Future[CsAtom]
  def load(id: String): Future[CsAtom]
  def update(id: String, explainerUpdate: ExplainerUpdate): Future[CsAtom]
  def publish(id: String): Future[CsAtom]
  def takeDown(id: String): Future[CsAtom]
  def getStatus(id: String, checkCapiStatus: Boolean): Future[PublicationStatus]

}
