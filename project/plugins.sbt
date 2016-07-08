// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

//resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

// Sbt plugins
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.10")

addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.3.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.8.4")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")