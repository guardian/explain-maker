package config

import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.typesafe.config.ConfigFactory

object Config {
  val conf = ConfigFactory.load()

  val stage = conf.getString("stage")

  val publicConfigForStage = conf.getConfig(stage)

  val pandaDomain = publicConfigForStage.getString("pandomain.domain")

  val awsCredentialsprovider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("composer"),
    new InstanceProfileCredentialsProvider
  )

  val tableName = s"explain-maker-preview-$stage-PHIL"



}