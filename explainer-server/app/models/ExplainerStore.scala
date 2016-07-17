package models

import cats.data.Xor
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsyncClient, AmazonDynamoDBClient}
import com.gu.scanamo.{Table, _}
import com.gu.scanamo.syntax._
import shared.Explainer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ExplainerStore {
  val defaultCredentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("composer"),
    new InstanceProfileCredentialsProvider
  )

  val dynamoDBClient: AmazonDynamoDBAsyncClient  = new AmazonDynamoDBAsyncClient(defaultCredentialsProvider).withRegion(EU_WEST_1)

  val explainersTable = Table[Explainer]("explainers-"+config.Config.stage)

  def all : Future[Seq[Explainer]] = {
//    val operations = for {
//      explainers: List[Xor[error.DynamoReadError, Explainer]] <- explainersTable.scan()
//    } yield {
//      explainers
//    }
    ScanamoAsync.exec(dynamoDBClient)(explainersTable.scan()).map(_.toList.flatMap(_.toOption))
  }

  def load(id: String): Future[Explainer] = {
    val operations = for {
      explainer <- explainersTable.get('id -> id)
    } yield {
      explainer
    }
    ScanamoAsync.exec(dynamoDBClient)(operations).map(_.flatMap(_.toOption).get)
  }

  def update(id: String, fieldSymbol: Symbol, value: String): Future[Explainer] = {
    assert(Set("headline","body").contains(fieldSymbol.name))
    val operations = for {
      _ <- explainersTable.update('id -> id, set(fieldSymbol -> value))
      explainer <- explainersTable.get('id -> id)
    } yield {
      explainer
    }
    ScanamoAsync.exec(dynamoDBClient)(operations).map(_.flatMap(_.toOption).get)
  }
}

