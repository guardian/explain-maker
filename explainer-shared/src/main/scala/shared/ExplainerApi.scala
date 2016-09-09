package shared

import shared.models.CsAtom
import shared.models.PublicationStatus.PublicationStatus

import scala.concurrent.Future

// shared interface
trait ExplainerApi {

  def load(id: String): Future[CsAtom]
  def update(id: String, fieldName: String, value: String): Future[CsAtom]
  def create(): Future[CsAtom]
  def takeDown(id: String): Future[CsAtom]
  def getStatus(id: String, checkCapiStatus: Boolean): Future[PublicationStatus]

  /*
   * Performs the migration of the current draft data to published state.
   */
  def publish(id: String): Future[CsAtom]

}
