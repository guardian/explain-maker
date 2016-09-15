package shared.models

sealed trait WorkflowStatus
case object Writers extends WorkflowStatus
case object Desk extends WorkflowStatus
case object Subs extends WorkflowStatus
case object Live extends WorkflowStatus

object WorkflowStatus {
  def apply(s: String): WorkflowStatus =  s match {
    case "Writers" => Writers
    case "Desk" => Desk
    case "Subs" => Subs
    case "Live" => Live
  }
}

case class WorkflowData(id: String, status: WorkflowStatus = Writers)