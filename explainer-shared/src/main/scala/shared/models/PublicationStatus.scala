package shared.models

sealed trait PublicationStatus extends Product with Serializable
final case object Draft extends PublicationStatus
final case object Available extends PublicationStatus
final case object UnlaunchedChanges extends PublicationStatus {
  override def toString = "Unlaunched Changes"
}
final case object TakenDown extends PublicationStatus {
  override def toString = "Taken Down"
}