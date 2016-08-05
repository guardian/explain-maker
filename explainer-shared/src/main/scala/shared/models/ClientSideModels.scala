package shared.models

import com.gu.contentatom.thrift._
import contentatom.explainer.{DisplayType, ExplainerAtom}
import shared.util.ExplainerAtomImplicits

case class CsExplainerAtom(title: String, body: String, displayType: String, tags: Option[List[String]])
case class CsUser(email: String, firstName: Option[String], lastName: Option[String])
case class CsChangeRecord(date: Long, user: Option[CsUser])
case class CsContentChangeDetails(lastModified: Option[CsChangeRecord], created: Option[CsChangeRecord], published: Option[CsChangeRecord], revision: Long)
case class CsAtom(id: String, data: CsExplainerAtom, contentChangeDetails: CsContentChangeDetails)

object CsExplainerAtom {
  def stringToDisplayType(s: String): DisplayType = {
    s match {
      case "FLAT" => DisplayType.Flat
      case _ => DisplayType.Expandable
    }
  }

}

object CsAtom extends ExplainerAtomImplicits {
  private implicit def userToCsUser(user: Option[User]): Option[CsUser]= user.map(u => CsUser(u.email, u.firstName, u.lastName))
  private implicit def changeRecordToCsChangeRecord(changeRecord: Option[ChangeRecord]): Option[CsChangeRecord]= changeRecord.map(cr => CsChangeRecord(cr.date, cr.user))
  private implicit def contentChangeDetailsToCsContentChangeDetails(contentChangeDetails: ContentChangeDetails): CsContentChangeDetails =
    CsContentChangeDetails(contentChangeDetails.lastModified, contentChangeDetails.created, contentChangeDetails.published, contentChangeDetails.revision)
  private implicit def explainerAtomToCsExplainerAtom(explainerAtom: ExplainerAtom): CsExplainerAtom = CsExplainerAtom(explainerAtom.title, explainerAtom.body, explainerAtom.displayType.name, explainerAtom.tags.map( _.toList ))

  def atomToCsAtom(atom: Atom) = CsAtom(atom.id, atom.tdata, atom.contentChangeDetails)

  implicit def csUserToUser(csUser: Option[CsUser]): Option[User] = csUser.map(u =>User(u.email, u.firstName, u.lastName))
  implicit def csChangeRecordToChangeRecord(csChangeRecord: Option[CsChangeRecord]): Option[ChangeRecord] = csChangeRecord.map(cr => ChangeRecord(cr.date, cr.user))
  implicit def csContentChangeDetailsToContentChangeDetails(csContentChangeDetails: CsContentChangeDetails): ContentChangeDetails =
    ContentChangeDetails(csContentChangeDetails.lastModified,
      csContentChangeDetails.created, csContentChangeDetails.published, csContentChangeDetails.revision)

  def csAtomToAtom(csAtom: CsAtom) = {
    val explainerAtom = ExplainerAtom(csAtom.data.title, csAtom.data.body, CsExplainerAtom.stringToDisplayType(csAtom.data.displayType), csAtom.data.tags)
    Atom(id = csAtom.id, atomType=AtomType.Explainer, defaultHtml = "",
      data = AtomData.Explainer(explainerAtom), contentChangeDetails = csAtom.contentChangeDetails)
  }

}

// play json converters - not currently needed

//  implicit val csUserFormats = Json.format[CsContentChangeDetails]
//  implicit val csChangeRecordFormats = Json.format[CsChangeRecord]
//  implicit val csExplainerAtomFormats = Json.format[CsExplainerAtom]
//  implicit val csAtomFormats = Json.format[CsAtom]
