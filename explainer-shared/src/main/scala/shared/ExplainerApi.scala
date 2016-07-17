package shared

import scala.concurrent.Future

// shared interface
trait ExplainerApi {
  def load(id: String): Future[Explainer]
  def update(id: String, fieldName: String, value: String): Future[Explainer]
  def create(): Future[Explainer]

  /*
    Performs the migration of the current draft data to published state.
   */
  def publish(id: String): Future[Explainer]
}
