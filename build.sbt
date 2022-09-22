import com.softwaremill.SbtSoftwareMillCommon.commonSmlBuildSettings

lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill.tapir.loom",
  scalaVersion := "3.2.0"
)

val tapirVersion = "1.1.0+62-76cd3353+20220922-1226-SNAPSHOT"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.12" % Test

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "tapir-loom")
  .aggregate(netty, nima)

lazy val netty: Project = (project in file("netty"))
  .settings(commonSettings: _*)
  .settings(
    name := "netty",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-server-tests" % tapirVersion % Test,
      scalaTest
    )
  )

lazy val nima: Project = (project in file("nima"))
  .settings(commonSettings: _*)
  .settings(
    name := "nima",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-server-tests" % tapirVersion % Test,
      "io.helidon.nima.webserver" % "helidon-nima-webserver" % "4.0.0-ALPHA1",
      scalaTest
    )
  )
