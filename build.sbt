name := "genesis"

version := "0.1"

scalaVersion := "2.12.8"

val remoteMavenRepo = "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
val localMavenRepo = "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
val scorexVersion = "4bc8c385-SNAPSHOT"

resolvers ++= Seq(remoteMavenRepo, localMavenRepo)

libraryDependencies ++= List(
  ("org.scorexfoundation" %% "scorex-core" % scorexVersion).exclude("ch.qos.logback", "logback-classic"),
  "com.typesafe.akka" %% "akka-actor" % "2.4.20"
)

