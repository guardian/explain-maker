package shared

import scala.concurrent.Future

// shared interface
trait ExplainerApi {
  def load(id: String): Future[ExplainerItem]
  def update(id: String, fieldName: String, value: String): Future[ExplainerItem]
  def create(): Future[ExplainerItem]

  /*
    Performs the migration of the current draft data to published state.
   */
  def publish(id: String): Future[ExplainerItem]
}
