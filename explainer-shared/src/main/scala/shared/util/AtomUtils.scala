package shared.util

import com.gu.contentatom.thrift.{Atom, AtomData, _}
import contentatom.explainer.ExplainerAtom

trait AtomDataTyper[D] {
  def getData(a: Atom): D
  def setData(a: Atom, newData: D): Atom
  def makeDefaultHtml(a: Atom): String
}

trait AtomImplicits[D] {
  val dataTyper: AtomDataTyper[D]
  implicit class AtomWithData(atom: Atom) {
    def tdata = dataTyper.getData(atom)
    def withData(data: D): Atom =
      dataTyper.setData(atom, data).updateDefaultHtml
    def updateData(f: D => D): Atom = withData(f(atom.tdata))
    def withRevision(f: Long => Long): Atom = atom.copy(
      contentChangeDetails = atom.contentChangeDetails.copy(
        revision = f(atom.contentChangeDetails.revision)
      )
    )
    def withRevision(newRevision: Long): Atom = withRevision(_ => newRevision)
    def updateDefaultHtml = atom.copy(defaultHtml = dataTyper.makeDefaultHtml(atom))
  }
}

trait ExplainerAtomImplicits extends AtomImplicits[ExplainerAtom] {
  val dataTyper = new AtomDataTyper[ExplainerAtom] {
    def getData(a: Atom) = a.data.asInstanceOf[AtomData.Explainer].explainer
    def setData(a: Atom, newData: ExplainerAtom) =
      a.copy(data = a.data.asInstanceOf[AtomData.Explainer].copy(explainer = newData))
    def makeDefaultHtml(a: Atom) = {
      val data = getData(a)
//      data.assets
//        .find(_.version == data.activeVersion)
//        .map(asset => views.html.ExplainerAtom.embedAsset(asset).toString)
//        .getOrElse(
          s"<div></div>"
//      )
    }
  }
}

object ExplainerAtomImplicits extends ExplainerAtomImplicits
