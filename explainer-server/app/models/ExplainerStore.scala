package models

import cats.data.Xor
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.regions.Regions._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsyncClient, AmazonDynamoDBClient}
import com.gu.scanamo.{Table, _}
import com.gu.scanamo.syntax._
import shared._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.joda.time.DateTime


object ExplainerStore {
  val defaultCredentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("composer"),
    new InstanceProfileCredentialsProvider
  )

  val dynamoDBClient: AmazonDynamoDBAsyncClient  = new AmazonDynamoDBAsyncClient(defaultCredentialsProvider).withRegion(EU_WEST_1)

  val explainersTable = Table[ExplainerItem]("explainers-"+config.Config.stage)

  def store(explainer: ExplainerItem): Unit = {
    Scanamo.put(dynamoDBClient)("explainers-"+config.Config.stage)(explainer)
  }

  def all : Future[Seq[ExplainerItem]] = {
    ScanamoAsync.exec(dynamoDBClient)(explainersTable.scan()).map(_.toList.flatMap(_.toOption))
  }

  def load(id: String): Future[ExplainerItem] = {
    val operations = for {
      explainer <- explainersTable.get('id -> id)
    } yield {
      explainer
    }
    ScanamoAsync.exec(dynamoDBClient)(operations).map(_.flatMap(_.toOption).get)
  }

  def update(id: String, fieldSymbol: Symbol, value: String): Future[ExplainerItem] = {
    val allowed_fields = Set(
      "title",
      "body"
    )
    assert(allowed_fields.contains(fieldSymbol.name))
    load(id).map{ explainer =>
      val newdraft = fieldSymbol.name match {
        case "title" => ExplainerFacet(value, explainer.draft.body, (new DateTime).getMillis())
        case "body" => ExplainerFacet(explainer.draft.title, value, (new DateTime).getMillis())
      }
      val updatedExplainer = ExplainerItem(
        explainer.id,
        newdraft,
        explainer.live
      )
      store(updatedExplainer)
      updatedExplainer
    }
  }

  def create(): Future[ExplainerItem] = {
    val uuid = java.util.UUID.randomUUID.toString
    val draft = ExplainerFacet("Default title @ " + (new DateTime).toString,"-",(new DateTime).getMillis())
    val explainer = new ExplainerItem(uuid,draft,None)
    Scanamo.put(dynamoDBClient)("explainers-"+config.Config.stage)(explainer)
    Future(explainer)
  }

}

