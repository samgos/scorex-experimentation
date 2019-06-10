import sbt.Keys._
import sbt._

name := "genesis"

version := "0.1"

scalaVersion := "2.12.8"

val remoteMavenRepo = "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
val localMavenRepo = "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
val scorexVersion = "4bc8c385-SNAPSHOT"

resolvers ++= Seq(remoteMavenRepo, localMavenRepo)

libraryDependencies ++= List(
  ("org.scorexfoundation" %% "scorex-core" % scorexVersion).exclude("ch.qos.logback", "logback-classic"),
  "org.scorexfoundation" %% "scorex-testkit" % scorexVersion % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.+" % "test",
  "org.scalactic" %% "scalactic" % "3.0.5" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.4.20",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.msgpack" %% "msgpack-scala" % "0.8.13",
  "org.scorexfoundation" %% "iodb" % "0.3.2"
)

mainClass in assembly := Some("zLedger")
mainClass in run := Some("zLedger")

fork := true

val opts = Seq(
  "-server",
  // JVM memory tuning for 2g ram
  "-Xms128m",
  "-Xmx2G",
  "-XX:+ExitOnOutOfMemoryError",
  // Java 9 support
  "-XX:+IgnoreUnrecognizedVMOptions",
  "--add-modules=java.xml.bind",

  // from https://groups.google.com/d/msg/akka-user/9s4Yl7aEz3E/zfxmdc0cGQAJ
  "-XX:+UseG1GC",
  "-XX:+UseNUMA",
  "-XX:+AlwaysPreTouch",

  // probably can't use these with jstack and others tools
  "-XX:+PerfDisableSharedMem",
  "-XX:+ParallelRefProcEnabled",
  "-XX:+UseStringDeduplication")

javaOptions in run ++= opts

homepage := Some(url("https://binarydistrict.com/ru/courses/blockchain-developer-30-01-17"))

licenses := Seq("CC0" -> url("https://creativecommons.org/publicdomain/zero/1.0/legalcode"))

mainClass in assembly := Some("zLedger")

assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.first
  case "module-info.class" => MergeStrategy.discard
  case  PathList("org", "scalatools", "testing", xs @ _*) => MergeStrategy.first
  case other => (assemblyMergeStrategy in assembly).value(other)
}
