import com.gu.riffraff.artifact.RiffRaffArtifact.autoImport._
import sbt.Project.projectToRef
import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
import play.sbt.PlayImport.PlayKeys._


lazy val clients = Seq(explainMakerClient)
lazy val scalaV = "2.11.8"
def env(key: String): Option[String] = Option(System.getenv(key))

lazy val awsVersion = "1.10.77"

lazy val explainMakerServer = (project in file("explainer-server")).enablePlugins(
  PlayScala,
  BuildInfoPlugin,
  RiffRaffArtifact,
  JDebPackaging
).settings(
  scalaVersion := scalaV,
  routesImport += "config.Routes._",
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd, gzip),
  resolvers ++= Seq("scalaz-bintray" at "https://dl.bintray.com/scalaz/releases", Resolver.sonatypeRepo("snapshots")),
  libraryDependencies ++= Seq(
    filters,
    "com.amazonaws" % "aws-java-sdk-core" % awsVersion,
    "com.gu" %% "scanamo" % "0.6.0",
    ws, // for panda
    cache,
    "com.gu" %% "pan-domain-auth-verification" % "0.3.0",
    "com.vmunier" %% "play-scalajs-scripts" % "0.3.0",
    "com.lihaoyi" %% "upickle" % "0.4.1",
    "com.lihaoyi" %% "autowire" % "0.2.5",
    "org.webjars" %% "webjars-play" % "2.5.0",
    "org.webjars" % "bootstrap" % "3.3.5",
    "org.webjars" % "jquery" % "2.1.4",
    "org.webjars" % "font-awesome" % "4.4.0",
    "org.webjars" % "tinymce" % "4.2.1",
    "com.gu" %% "atom-publisher-lib" % "0.1.3-SNAPSHOT",
    "com.twitter" %% "scrooge-core" % "4.5.0",
    "com.gu" %% "scanamo-scrooge" % "0.1.2",
    "com.amazonaws" % "aws-java-sdk-ec2" % awsVersion,
    "com.gu" % "kinesis-logback-appender" % "1.2.0",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.2",
    "com.gu" %% "content-api-client" % "9.5"
  ),
  sources in (Compile,doc) := Seq.empty, publishArtifact in (Compile, packageDoc) := false, // Don't do slow ScalaDoc step for anything but a library!
  serverLoading in Debian := Systemd,
  debianPackageDependencies := Seq("openjdk-8-jre-headless"),
  javaOptions in Universal ++= Seq(
    "-Dpidfile.path=/dev/null",
    "-J-XX:MaxRAMFraction=2",
    "-J-XX:InitialRAMFraction=2",
    "-J-XX:MaxMetaspaceSize=500m",
    "-J-XX:+PrintGCDetails",
    "-J-XX:+PrintGCDateStamps"
  ),
  maintainer := "Digital CMS Team <digitalcms.dev@guardian.co.uk>",
  name := "explain-maker",
  packageSummary := "Explain maker tool",
  packageName in Universal := normalizedName.value,
  packageDescription := """Editor tool to create and update explainer 'atoms'""",
  riffRaffPackageType := (packageZipTarball in config("universal")).value,
  riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
  riffRaffUploadManifestBucket := Option("riffraff-builds"),
  riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV"),
  riffRaffManifestBranch := env("BRANCH_NAME").getOrElse("unknown_branch"),
  riffRaffManifestVcsUrl  := "git@github.com:guardian/explain-maker.git",
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    BuildInfoKey.constant("gitCommitId", env("BUILD_VCS_NUMBER") getOrElse (try {
      "git rev-parse HEAD".!!.trim
    } catch {
      case e: Exception => "unknown"
    })),
    BuildInfoKey.constant("buildNumber", env("BUILD_NUMBER") getOrElse "DEV"),
    BuildInfoKey.constant("buildTime", System.currentTimeMillis)
  ),
  buildInfoPackage := "app"
).aggregate(clients.map(projectToRef): _*).
  dependsOn(explainMakerSharedJvm)

lazy val explainMakerClient = (project in file("explainer-client")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.1",
    "com.lihaoyi" %%% "scalatags" % "0.5.2",
    "com.lihaoyi" %%% "scalarx" % "0.2.8",
    "be.doeraene" %%% "scalajs-jquery" % "0.8.0",
    "com.lihaoyi" %%% "autowire" % "0.2.5",
    "com.lihaoyi" %%% "upickle" % "0.4.1",
    "fr.hmil" %%% "roshttp" % "1.0.0"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(explainMakerSharedJs)

lazy val explainMakerShared = (crossProject.crossType(CrossType.Pure) in file("explainer-shared")).
  settings(scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      "com.gu" %% "content-atom-model" % "2.4.3"
    ))
    .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val explainMakerSharedJvm = explainMakerShared.jvm
lazy val explainMakerSharedJs = explainMakerShared.js

// loads the jvm project at sbt startup
onLoad in Global := (Command.process("project explainMakerServer", _: State)) compose (onLoad in Global).value

