package db

import cats.data.Xor
import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.gu.contentatom.thrift.Atom
import com.gu.scanamo.error.{DynamoReadError, TypeCoercionError}
import com.gu.scanamo.{DynamoFormat, Scanamo, ScanamoAsync}
import config.Config
import com.gu.scanamo.{Table, _}
import com.gu.scanamo.syntax._
//import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import com.twitter.scrooge.CompactThriftSerializer
import contentatom.explainer.{DisplayType, ExplainerAtom}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ExplainerDB {
  implicit def seqFormat[T](implicit f: DynamoFormat[T]): DynamoFormat[Seq[T]] =
    DynamoFormat.xmap[Seq[T], List[T]](l => Xor.right(l.toSeq))(_.toList)

  // you can't use BinaryThriftStructSerializer from crooge-serializer
  // if Thrift is greater than 0.9.0 as they use a method that has
  // been removed (see https://github.com/twitter/scrooge/issues/203)
  // Also the JsonThriftserializer is one way only (it doesn't store
  // the Thrift type data because it uses the TSimpleJsonProtocol)

  private val atomSerializer = CompactThriftSerializer(Atom)

  private def rowToAtom(row: AtomRow): Xor[DynamoReadError, Atom] = try {
    Xor.Right(atomSerializer.fromString(row.atom))
  } catch {
    case err: org.apache.thrift.TException => Xor.Left(TypeCoercionError(err))
  }

  case class AtomRow(id: String, atom: String)

  object AtomRow {
    def apply(atom: Atom): AtomRow = AtomRow(
      atom.id, atomSerializer.toString(atom)
    )
  }

  implicit val dynamoFormat: DynamoFormat[Atom] =
    DynamoFormat.xmap(rowToAtom _)(AtomRow.apply _)(DynamoFormat[AtomRow]) // <- just saving a new implicit here


  val dynamoDBClient: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient(Config.awsCredentialsprovider).withRegion(EU_WEST_1)
  val explainersTable  = Table[Atom](Config.tableName)

  def store(explainer: Atom): Unit = {
    Scanamo.put(dynamoDBClient)(config.Config.tableName)(explainer)
  }

  def all : Future[Seq[Atom]] = {
    ScanamoAsync.exec(dynamoDBClient)(explainersTable.scan()).map(_.toList.flatMap(_.toOption))
  }

  def load(id: String): Future[Atom] = {
    val operations = for {
      explainer <- explainersTable.get('id -> id)
    } yield {
      explainer
    }
    ScanamoAsync.exec(dynamoDBClient)(operations).map(_.flatMap(_.toOption).get)
  }

}