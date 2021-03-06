package config

import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.kinesis.AmazonKinesisClient
import play.api.Configuration
import services.AwsInstanceTags

@Singleton
class Config @Inject() (conf: Configuration) extends AwsInstanceTags {
  
  val stage = readTag("Stage") getOrElse "DEV"
  val app = readTag("App") getOrElse "explain-maker"
  val stack = readTag("Stack") getOrElse "composer"

  def configValueForStage(path: String) = conf.getString(s"$stage.$path")

  def fetchOrErrorOptionalProperty(propertyEnabled: Boolean, path: String, errorMessage: String) = {
    if (propertyEnabled) {
      configValueForStage(path).getOrElse(sys.error(errorMessage))
    } else ""
  }

  val pandaDomain = configValueForStage("pandomain.domain").get
  val appDomainName = "explainers"

  val capiKey = configValueForStage("capi.key").get
  val capiUrl = stage match {
    case "CODE" => "http://content.code.dev-guardianapis.com"
    case "PROD" => "https://content.guardianapis.com"
    case "DEV" => "http://content.guardianapis.com"
  }

  val capiPreviewUrl = stage match {
    case "CODE" => "preview.content.code.dev-guardianapis.com"
    case "PROD" => "preview.content.guardianapis.com"
    case "DEV" => "preview.content.guardianapis.com"
  }

  val capiPreviewUsername = conf.getString("capi.preview.username")
  val capiPreviewPassword = conf.getString("capi.preview.password")

  val presenceEnabled = conf.getBoolean("enable.presence") getOrElse true
  val presenceEndpointURL = fetchOrErrorOptionalProperty(presenceEnabled, "presence.endpoint", "presence endpoint required when presence enabled")

  val interactiveUrl = conf.getString("interactive.url")

  val ophanUrl = "<<unused>>"

  val fastlyPurgingEnabled = stage =="PROD"
  val fastlyAPIKey = fetchOrErrorOptionalProperty(fastlyPurgingEnabled, "fastly.apikey", "fastly api key required when fastly purging enabled")

  lazy val region = {
    val r = conf.getString("aws.region").map(Regions.fromName).getOrElse(Regions.EU_WEST_1)
    Region.getRegion(r)
  }

  val awsCredentialsprovider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("composer"),
    new InstanceProfileCredentialsProvider
  )

  val publishToKinesis = conf.getBoolean("enable.kinesis.publishing") getOrElse true

  val previewKinesisStreamName = fetchOrErrorOptionalProperty(publishToKinesis, "kinesis.streamName.preview", "preview stream name required when kinesis publishing enabled")
  val liveKinesisStreamName = fetchOrErrorOptionalProperty(publishToKinesis, "kinesis.streamName.live", "live stream name required when kinesis publishing enabled")
  val previewReindexKinesisStreamName = fetchOrErrorOptionalProperty(publishToKinesis, "kinesis.streamName.reindex-preview", "reindex preview stream name required")
  val liveReindexKinesisStreamName = fetchOrErrorOptionalProperty(publishToKinesis, "kinesis.streamName.reindex-live", "reindex live stream name required")


  val elkLoggingEnabled = conf.getBoolean("enable.elk.logging") getOrElse true
  val elkKinesisStream = fetchOrErrorOptionalProperty(elkLoggingEnabled, "kinesis.streamName.elk", "elk stream name required when elk logging enabled")

  lazy val kinesisClient = region.createClient(
    classOf[AmazonKinesisClient],
    awsCredentialsprovider,
    null
  )

  val dynamoClient = region.createClient(
    classOf[AmazonDynamoDBClient],
    awsCredentialsprovider,
    null
  )

  val previewTableName = s"explain-maker-preview-$stage"
  val liveTableName = s"explain-maker-live-$stage"
  val workflowDataTableName = s"explain-maker-workflow-data-$stage"

  val ExplainListPageSize = 20

}