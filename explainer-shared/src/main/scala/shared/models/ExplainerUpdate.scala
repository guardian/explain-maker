package shared.models


case class ExplainerUpdate(field: UpdateField.UpdateField, value: String)

object UpdateField {
  sealed trait UpdateField {
    def name: String
  }
  case object Title extends UpdateField { val name = "title"}
  case object Body extends UpdateField { val name = "body"}
  case object DisplayType extends UpdateField { val name = "displayType"}
  case object AddTag extends UpdateField { val name = "addTag"}
  case object RemoveTag extends UpdateField { val name = "removeTag"}
}
