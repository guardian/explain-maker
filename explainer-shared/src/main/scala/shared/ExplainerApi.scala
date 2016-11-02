package shared

import shared.models._

import scala.concurrent.Future

// shared interface
trait ExplainerApi {

  def create(): Future[CsAtom]
  def load(id: String): Future[CsAtom]
  def update(id: String, explainerUpdate: ExplainerUpdate): Future[CsAtom]
  def publish(id: String): Future[CsAtom]
  def takeDown(id: String): Future[CsAtom]
  def delete(id: String): Future[Unit]
  def getStatus(id: String): Future[PublicationStatus]

  def getWorkflowData(id:String): WorkflowData
  def setWorkflowData(workflowData: WorkflowData)

  def getTrackingTags(): Future[Seq[CsTag]]

}
