package shared.models


object PublicationStatus {
  sealed trait PublicationStatus {
    def name: String
  }
  case object Draft extends PublicationStatus { val name = "draft"}
  case object Available extends PublicationStatus { val name = "available"}
  case object UnlaunchedChanges extends PublicationStatus { val name = "unlaunchedChanges"}
  case object TakenDown extends PublicationStatus { val name = "takenDown"}
}
