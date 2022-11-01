import com.softwaremill.SbtSoftwareMillCommon.commonSmlBuildSettings
import com.softwaremill.Publish.ossPublishSettings

val scala2_13 = "2.13.8"
val scala2 = List(scala2_13)
val scala3 = List("3.2.0")
val scalaAll = scala2 ++ scala3

excludeLintKeys in Global ++= Set(ideSkipProject)

lazy val commonSettings = commonSmlBuildSettings ++ ossPublishSettings ++ Seq(
  organization := "com.softwaremill.sttp.tapir",
  ideSkipProject := (scalaVersion.value != scala2_13),
  javaOptions += "--enable-preview",
  fork := true
)

val tapirVersion = "1.1.4"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.13" % Test

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

lazy val nima = (projectMatrix in file("nima"))
  .settings(commonSettings: _*)
  .settings(
    name := "tapir-nima-server",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-server-tests" % tapirVersion % Test,
      "io.helidon.nima.webserver" % "helidon-nima-webserver" % "4.0.0-ALPHA1",
      scalaTest
    )
  )
  .jvmPlatform(scalaVersions = scalaAll)
