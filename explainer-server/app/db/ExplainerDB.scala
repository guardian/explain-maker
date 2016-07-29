package db

import javax.inject.Inject

import cats.data.Xor
import com.gu.contentatom.thrift._
import config.Config
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.gu.atom.data.DynamoDataStore
import contentatom.explainer.ExplainerAtom


class ExplainerDB @Inject() (config: Config) {

  val dynamoDataStore = new DynamoDataStore[ExplainerAtom](config.dynamoClient, config.tableName) {
    def fromAtomData = { case AtomData.Explainer(data) => data }
    def toAtomData(data: ExplainerAtom) = AtomData.Explainer(data)
  }


  def create(explainer: Atom) = {
    dynamoDataStore.createAtom(explainer)
  }

  def store(explainer: Atom): Unit = {
    dynamoDataStore.updateAtom(explainer)
  }

  def all : Future[Seq[Atom]] = {
    Future(dynamoDataStore.listAtoms match {
      case Xor.Right(atoms) => atoms.toSeq
      case _ => Nil
    })
  }

  def load(id: String): Future[Atom] = {
    Future(dynamoDataStore.getAtom(id).get)

  }

}
