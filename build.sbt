import sbt.Project.projectToRef
import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd

lazy val clients = Seq(explainerClient)
lazy val scalaV = "2.11.8"

def env(key: String): Option[String] = Option(System.getenv(key))

lazy val explainerServer = (project in file("explainer-server")).enablePlugins(
  PlayScala,
  BuildInfoPlugin,
  RiffRaffArtifact,
  JDebPackaging
).settings(
  scalaVersion := scalaV,
  routesImport += "config.Routes._",
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd, gzip),
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    filters,
    "com.gu" %% "scanamo" % "0.6.0",
    "com.vmunier" %% "play-scalajs-scripts" % "0.3.0",
    "com.lihaoyi" %% "upickle" % "0.4.1",
    "com.lihaoyi" %% "autowire" % "0.2.5",
    "org.webjars" %% "webjars-play" % "2.4.0",
    "org.webjars" % "bootstrap" % "3.3.5",
    "org.webjars" % "jquery" % "2.1.4",
    "org.webjars" % "font-awesome" % "4.4.0"
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
    "-J-XX:+PrintGCDateStamps",
    s"-J-Xloggc:/var/log/${name.value}/gc.log"
  ),
  maintainer := "Membership Discovery <membership.dev@theguardian.com>",
  packageSummary := "Explainer tool",
  packageDescription := """Editor tool to create and update the Explainer 'atoms'""",
  riffRaffPackageName := "explain-maker",
  riffRaffPackageType := (packageBin in Debian).value,
  riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
  riffRaffUploadManifestBucket := Option("riffraff-builds"),
  riffRaffManifestProjectName := "editorial-tools:explainer",
  riffRaffManifestBranch := env("BRANCH_NAME").getOrElse("unknown_branch"),
  riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV"),
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
  dependsOn(explainerSharedJvm)

lazy val explainerClient = (project in file("explainer-client")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.1",
    "com.lihaoyi" %%% "scalatags" % "0.5.2",
    "com.lihaoyi" %%% "scalarx" % "0.2.8",
    "be.doeraene" %%% "scalajs-jquery" % "0.8.0",
    "com.lihaoyi" %%% "autowire" % "0.2.5",
    "com.lihaoyi" %%% "upickle" % "0.4.1"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(explainerSharedJs)

lazy val explainerShared = (crossProject.crossType(CrossType.Pure) in file("explainer-shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val explainerSharedJvm = explainerShared.jvm
lazy val explainerSharedJs = explainerShared.js

// loads the jvm project at sbt startup
onLoad in Global := (Command.process("project explainerServer", _: State)) compose (onLoad in Global).value

