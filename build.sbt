import scala.util.Try
import sbtbuildinfo.BuildInfoKey.action
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoOptions, buildInfoPackage}
import sbtbuildinfo.{BuildInfoKey, BuildInfoOption}

val CatsVersion = "2.6.1"
val CatsEffectVersion = "3.3.12"
val Http4sVersion = "0.23.12"
val CirceVersion = "0.15.0-M1"
val CirceGenericExVersion = "0.15.0-M1"
val CirceConfigVersion = "0.8.0"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.11"
val CatsEffectTestingVersion = "1.4.0"
val FlywayVersion = "8.5.13"
val DoobieVersion = "1.0.0-RC2"
val TypesafeConfigVersion = "1.4.1"
val QuillVersion = "4.0.0"
val prometheusVersion = "0.16.0"
val sttpVersion = "3.6.2"
val tapirVersion = "1.0.1"
val macwireVersion = "2.5.7"
val testContainersVersion = "1.17.3"
val apacheCommonsVersion = "3.12.0"

val httpDependencies = Seq(
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "org.http4s" %% "http4s-ember-server" % Http4sVersion,
  "org.http4s" %% "http4s-ember-client" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion,
  "com.softwaremill.sttp.client3" %% "http4s-backend" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "circe" % sttpVersion
)

val monitoringDependencies = Seq(
  "io.prometheus" % "simpleclient" % prometheusVersion,
  "io.prometheus" % "simpleclient_hotspot" % prometheusVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion
)

val jsonDependencies = Seq(
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion
)

val loggingDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-core" % LogbackVersion,
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  // TODO fix: logback classic being evicted due to other dependencies in scope
  "org.slf4j" % "slf4j-simple" % "2.0.0-alpha1"
)

val catsDependencies = Seq(
  "org.typelevel" %% "cats-core" % CatsVersion,
  "org.typelevel" %% "cats-effect" % CatsEffectVersion
)

val dbDependencies = Seq(
  "org.tpolecat" %% "doobie-core" % DoobieVersion,
  "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
  "org.flywaydb" % "flyway-core" % FlywayVersion,
  "org.postgresql" % "postgresql" % "42.3.1",
  "io.getquill" %% "quill-doobie" % QuillVersion
)

val macwireDependencies = Seq(
  "com.softwaremill.macwire" %% "macros" % macwireVersion,
  "com.softwaremill.macwire" %% "util" % macwireVersion
)

val apiDocsDependencies = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
)

val testDependencies = Seq(
  "org.typelevel" %% "cats-effect-testing-scalatest" % CatsEffectTestingVersion % Test,
  "org.tpolecat" %% "doobie-scalatest" % DoobieVersion % Test,
  "org.testcontainers" % "jdbc" % testContainersVersion % Test,
  "org.testcontainers" % "postgresql" % testContainersVersion % Test
)

val commonDependencies = Seq(
  "org.apache.commons" % "commons-lang3" % apacheCommonsVersion,
  "com.typesafe" % "config" % TypesafeConfigVersion
)

libraryDependencies ++= loggingDependencies

lazy val dockerSettings = Seq(
  dockerExposedPorts := Seq(8080),
  dockerBaseImage := "openjdk:11-jre-slim",
  dockerUpdateLatest := true
)

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    action("lastCommitHash") {
      import scala.sys.process._
      // if the build is done outside of a git repository, we still want it to succeed
      Try("git rev-parse HEAD".!!.trim).getOrElse("?")
    }
  ),
  buildInfoPackage := "io.lgos.template.version",
  buildInfoObject := "BuildInfo"
)

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .settings(dockerSettings)
  .settings(buildInfoSettings)
  .settings(
    organization := "io.lgos",
    name := "template",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "3.1.2",
    mainClass := Some("io.lgos.template.Main"),
    libraryDependencies ++=
      dbDependencies ++
        httpDependencies ++
        jsonDependencies ++
        apiDocsDependencies ++
        monitoringDependencies ++
        macwireDependencies ++
        loggingDependencies ++
        testDependencies ++ commonDependencies,
    excludeDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-collection-compat_2.13"
    )
  )
