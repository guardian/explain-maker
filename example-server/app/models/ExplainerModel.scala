package models

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
    new ProfileCredentialsProvider("membership"),
    new InstanceProfileCredentialsProvider
  )

  val dynamoDBClient: AmazonDynamoDBAsyncClient  = new AmazonDynamoDBAsyncClient(defaultCredentialsProvider).withRegion(EU_WEST_1)

  val explainersTable = Table[Explainer]("explainers")

  def update(id: Long, fieldSymbol: Symbol, value: String): Future[Explainer] = {
    val operations = for {
      _ <- explainersTable.update('id -> id, set(fieldSymbol -> value))
      explainer <- explainersTable.get('id -> id)
    } yield {
      println(explainer)
      explainer
    }
    ScanamoAsync.exec(dynamoDBClient)(operations).map(_.flatMap(_.toOption).get)
  }
}

