package shared

case class Explainer(id: String, headline: String, body: String, last_update_time_milli: Option[Long])

case class ExplainerUpdate(field: String, value: String)
