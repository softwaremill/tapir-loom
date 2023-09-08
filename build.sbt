import com.softwaremill.SbtSoftwareMillCommon.commonSmlBuildSettings
import com.softwaremill.Publish.ossPublishSettings

val scala2_13 = "2.13.11"
val scala2 = List(scala2_13)
val scala3 = List("3.3.1")
val scalaAll = scala2 ++ scala3

excludeLintKeys in Global ++= Set(ideSkipProject)

lazy val commonSettings = commonSmlBuildSettings ++ ossPublishSettings ++ Seq(
  organization := "com.softwaremill.sttp.tapir",
  ideSkipProject := (scalaVersion.value != scala2_13),
  javaOptions += "--enable-preview",
  fork := true
)

val tapirVersion = "1.6.4"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.16" % Test

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "tapir-loom", scalaVersion := scala2_13)
  .aggregate(netty.projectRefs ++ nima.projectRefs: _*)

lazy val netty = (projectMatrix in file("netty"))
  .settings(commonSettings: _*)
  .settings(
    name := "tapir-netty-server-id",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-server-tests" % tapirVersion % Test,
      scalaTest
    )
  )
  .jvmPlatform(scalaVersions = scalaAll)

lazy val helidonVersion = "4.0.0-M1"
lazy val nima = (projectMatrix in file("nima"))
  .settings(commonSettings: _*)
  .settings(
    name := "tapir-nima-server",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-server-tests" % tapirVersion % Test,
      "io.helidon.nima.webserver" % "helidon-nima-webserver" % helidonVersion,
      scalaTest
    ) ++ loggerDependencies.map(_ % Test)
  )
  .jvmPlatform(scalaVersions = scalaAll)

lazy val loggerDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.4.9",
  "io.helidon.logging" % "helidon-logging-slf4j" % helidonVersion // to see logs from helidon
)
