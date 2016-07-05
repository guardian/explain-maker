package config

import com.typesafe.config.ConfigFactory

object Config {
  val conf = ConfigFactory.load()

  val stage = conf.getString("stage")
}