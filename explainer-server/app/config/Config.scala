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
  val capiKey = configValueForStage("capi.key")

  val interactiveUrl = conf.getString("interactive.url")

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

  val tableName = s"explain-maker-preview-$stage"

}