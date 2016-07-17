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

import org.joda.time.DateTime


object ExplainerStore {
  val defaultCredentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("composer"),
    new InstanceProfileCredentialsProvider
  )

  val dynamoDBClient: AmazonDynamoDBAsyncClient  = new AmazonDynamoDBAsyncClient(defaultCredentialsProvider).withRegion(EU_WEST_1)

  val explainersTable = Table[Explainer]("explainers-"+config.Config.stage)

  def all : Future[Seq[Explainer]] = {
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
      _ <- explainersTable.update('id -> id, set(Symbol("last_update_time_milli") -> (new DateTime).getMillis()))
      explainer <- explainersTable.get('id -> id)
    } yield {
      explainer
    }
    ScanamoAsync.exec(dynamoDBClient)(operations).map(_.flatMap(_.toOption).get)
  }

  def create(): Future[Explainer] = {
    val uuid = java.util.UUID.randomUUID.toString
    val headline = "Default headline @ " + (new DateTime).toString
    val updatetime = Some((new DateTime).getMillis())
    val explainer = new Explainer(uuid, headline, "-",updatetime) // body field cannot be empty string
    Scanamo.put(dynamoDBClient)("explainers-"+config.Config.stage)(explainer)
    Future(explainer)
  }

}

