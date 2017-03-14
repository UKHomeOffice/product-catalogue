import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy

name := "cjp-catalogue"

version := "1.0-SNAPSHOT"

val libs = Seq(
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.json4s" %% "json4s-native" % "3.2.5",
  "org.json4s" %% "json4s-ext" % "3.2.5",
  "org.json4s" %% "json4s-jackson" % "3.2.5",
  "commons-codec" % "commons-codec" % "1.9",
  "com.github.nscala-time" %% "nscala-time" % "0.2.0",
  "org.mongodb" %% "casbah-core" % "2.6.4",
  "org.mongodb" % "mongo-java-driver" % "2.11.3",
  "com.novus" %% "salat-core" % "1.9.5",
  "com.novus" %% "salat-util" % "1.9.5",
  "joda-time" % "joda-time" % "2.3",
  "net.logstash.logback" % "logstash-logback-encoder" % "3.0",
  "org.yaml" % "snakeyaml" % "1.13"
)

val miscSettings = Seq(
  javaOptions += "-Xmx333M",
  scalacOptions ++= Seq("-feature", "-language:_"),
  parallelExecution in ScctPlugin.ScctTest := false,
  publishArtifact in(Compile, packageDoc) := false,
  publishArtifact in packageDoc := false,
  sources in(Compile, doc) := Seq.empty,
  parallelExecution in Test := false,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "test-reports"),
  javaOptions in Test ++= Seq("-Xmx1024M", "-XX:PermSize=256M", "-XX:+CMSClassUnloadingEnabled")
)

lazy val catalogue = project.in(file("."))
  .enablePlugins(PlayScala, JvmPlugin)
  .settings(miscSettings: _*)
  .settings(ScctPlugin.instrumentSettings, ScctPlugin.mergeReportSettings)
  .settings(incOptions := incOptions.value.withNameHashing(nameHashing = true))
  .settings(
    libraryDependencies ++= libs,
    dependencyOverrides ++= Set( // clear up the classpath on assembly
      "io.netty" % "netty" % "3.9.3.Final",
      "org.slf4j" % "slf4j-api" % "1.7.6",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.2",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.3.2"
    ),
    libraryDependencies ~= {
      _ map {
        case m if m.organization == "com.typesafe.play" =>
          m.exclude("commons-logging", "commons-logging")
        case m => m
      }
    })
  .settings(
    mainClass in assembly := Some("play.core.server.NettyServer"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
    assemblyMergeStrategy in assembly := {
      case "play/core/server/ServerWithStop.class" => MergeStrategy.first
      case other => (assemblyMergeStrategy in assembly).value(other)
    })










