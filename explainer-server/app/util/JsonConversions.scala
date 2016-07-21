package util

import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.media.{Asset, MediaAtom}
import com.twitter.scrooge.ThriftEnum
import contentatom.explainer.{DisplayType, ExplainerAtom}
import play.api.libs.functional.syntax._
import play.api.libs.json._


// play json conversions for thrift objects NOTE: not currently used in the application
object JsonConversions {


  // generic code
  implicit val thriftEnumWrites = Writes[ThriftEnum](thriftEnum => JsString(thriftEnum.name.toLowerCase))
  implicit val displayTypeReads = Reads[DisplayType] {
    case (JsString(s)) => JsSuccess(DisplayType.valueOf(s).get)
    case _ => JsError("unable to parse displaytype")
  }
  implicit val atomTypeReads = Reads[AtomType] {
    case (JsString(s)) => JsSuccess(AtomType.valueOf(s).get)
    case _ => JsError("unable to parse atomType")
  }

  //media atom

  implicit val mediaAsset = (
    (__ \ "id").write[String] and
      (__ \ "version").write[Long] and
      (__ \ "platform").write[String] and
      (__ \ "assetType").write[String]
    ) { asset: Asset =>
    asset match {
      case Asset(assetType, version, id, platform, mimeType) => (id, version, platform.name, assetType.name)
    }
  }

  implicit val atomDataMedia = (
    (__ \ "assets").write[Seq[Asset]] and
      (__ \ "activeVersion").write[Long]
    ) { mediaAtom: MediaAtom =>
    (mediaAtom.assets, mediaAtom.activeVersion.get)
  }

  //explainer atom

  implicit val atomDataExplainer = (
    (__ \ "title").write[String] and
      (__ \ "body").write[String] and
      (__ \ "displayType").write[DisplayType]
    ) { explainerAtom: ExplainerAtom =>
    (explainerAtom.title, explainerAtom.body, explainerAtom.displayType)
  }

  implicit val atomDataWrites = Writes[AtomData] {
    case AtomData.Media(mediaAtom) => Json.toJson(mediaAtom)
    case AtomData.Explainer(explainerAtom) => Json.toJson(explainerAtom)
    case _ => JsString("unknown")
  }

  implicit val explainerAtomReads = Reads[AtomData] {
    case (js: JsObject) => {
      JsSuccess(AtomData.Explainer(ExplainerAtom(
        (js \ "title").as[String],
        (js \ "body").as[String],
        (js \ "displayType").as[DisplayType]
      )))
    }
    case _ => JsError("not a jsobject")
  }

  // content change details

  implicit val userReads: Reads[User] = (
    (__ \ "email").read[String] and
      (__ \ "firstName").readNullable[String] and
      (__ \ "lastName").readNullable[String]
    ) (User.apply _)

  implicit val userWrites: Writes[User] = (
    (__ \ "email").write[String] and
      (__ \ "firstName").writeNullable[String] and
      (__ \ "lastName").writeNullable[String]
    ) { user: User =>
    (user.email, user.firstName, user.lastName)
  }


  implicit val changeRecordReads: Reads[ChangeRecord] = (
    (__ \ "date").read[Long] and
      (__ \ "user").readNullable[User]
    ) (ChangeRecord.apply _)

  implicit val changeRecordWrites: Writes[ChangeRecord] = (
    (__ \ "date").write[Long] and
      (__ \ "user").writeNullable[User]
    ) { cr: ChangeRecord =>
    (cr.date, cr.user)
  }

  implicit val contentChangeDetailsReads: Reads[ContentChangeDetails] = (
    (__ \ "lastModified").readNullable[ChangeRecord] and
      (__ \ "created").readNullable[ChangeRecord] and
      (__ \ "published").readNullable[ChangeRecord] and
      (__ \ "revision").read[Long]
    ) (ContentChangeDetails.apply _)

  implicit val contentChangeDetailsWrites: Writes[ContentChangeDetails] = (
    (__ \ "lastModified").writeNullable[ChangeRecord] and
      (__ \ "created").writeNullable[ChangeRecord] and
      (__ \ "published").writeNullable[ChangeRecord] and
      (__ \ "revision").write[Long]
    ) { ccd: ContentChangeDetails =>
    (ccd.lastModified, ccd.created, ccd.published, ccd.revision)
  }

  // flags

  implicit val flagReads: Reads[Flags] =
    (JsPath \ "suppressFurniture").readNullable[Boolean].map(v => Flags(v))


  implicit val flagWrites = Writes[Flags](f => Json.toJson(f.suppressFurniture))

  // atom


  implicit val atomWrites = (
    (__ \ "id").write[String] and
      (__ \ "type").write[AtomType] and
      (__ \ "labels").write[Seq[String]] and
      (__ \ "defaultHtml").write[String] and
      (__ \ "data").write[AtomData] and
      (__ \ "contentChangeDetails").write[ContentChangeDetails] and
      (__ \ "flags").writeNullable[Flags]
    ) { atom: Atom => (atom.id, atom.atomType, atom.labels, atom.defaultHtml, atom.data, atom.contentChangeDetails, atom.flags)
  }


  implicit val atomReads = Reads[Atom] {
    case (json: JsObject) => {
      // TODO: Make this generic by using a different reads for atomdata depending on the type of atom
      JsSuccess(Atom.apply(
        (json \ "id").as[String],
        (json \ "type").as[AtomType],
        (json \ "labels").as[Seq[String]],
        (json \ "defaultHtml").as[String],
        (json \ "data").as[AtomData],
        (json \ "contentChangeDetails").as[ContentChangeDetails],
        (json \ "flags").asOpt[Flags]
      ))
    }
    case _ => JsError("not a jsobject")
  }


}