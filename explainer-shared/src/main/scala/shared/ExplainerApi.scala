package shared

import scala.concurrent.Future

// shared interface
trait ExplainerApi {
  def load(id: String): Future[Explainer]

  def update(id: String, fieldName: String, value: String): Future[Explainer]
}
