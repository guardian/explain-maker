package util

import com.gu.contentatom.thrift.Atom


object HelperFunctions {
  def getCreatedByString(explainer: Atom) = {
    val firstNameLastName = for {
      created <- explainer.contentChangeDetails.created
      user <- created.user
      firstName <- user.firstName
      lastName <- user.lastName
    } yield {
      s"$firstName $lastName"
    }
    firstNameLastName.getOrElse("-")
  }

  def isPublished(explainer: Atom) = {
    if(explainer.contentChangeDetails.published.isDefined) "Published" else "Draft"
  }
}