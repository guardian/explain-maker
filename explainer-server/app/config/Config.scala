package config

import com.gu.pandomainauth.model.AuthenticatedUser
import com.typesafe.config.ConfigFactory

object Config {
  val conf = ConfigFactory.load()

  val stage = conf.getString("stage")

  val publicConfigForStage = conf.getConfig(stage)

  val pandaDomain = publicConfigForStage.getString("pandomain.domain")


}