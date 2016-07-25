package config

import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.kinesis.AmazonKinesisClient
import play.api.Configuration
import services.{AWS, AwsInstanceTags}

@Singleton
class Config @Inject() (conf: Configuration) extends AwsInstanceTags {


  val stage = readTag("Stage") getOrElse "DEV"

  def configValueForStage(value: String) = conf.getString(s"$stage.$value")

  val pandaDomain = configValueForStage("pandomain.domain").get


  lazy val region = {
    val r = conf.getString("aws.region").map(Regions.fromName).getOrElse(Regions.EU_WEST_1)
    Region.getRegion(r)
  }

  val awsCredentialsprovider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("composer"),
    new InstanceProfileCredentialsProvider
  )

  val publishToKinesis = conf.getBoolean("enable.kinesis.publishing") getOrElse true

  val previewKinesisStreamName = if (publishToKinesis) configValueForStage("kinesis.streamName.preview").get else ""
  val liveKinesisStreamName = if (publishToKinesis) configValueForStage("kinesis.streamName.live").get else ""

  lazy val kinesisClient = region.createClient(
    classOf[AmazonKinesisClient],
    awsCredentialsprovider,
    null
  )


  val tableName = s"explain-maker-preview-$stage"





}