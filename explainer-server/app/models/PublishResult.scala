package models

sealed trait PublishResult

case object Success extends PublishResult
case object Fail extends PublishResult
case object Disabled extends PublishResult
