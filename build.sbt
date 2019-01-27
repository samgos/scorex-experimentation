name := "genesis"

version := "0.1"

scalaVersion := "2.12.8"

val remoteMavenRepo = "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
val localMavenRepo = "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"

resolvers ++= Seq(remoteMavenRepo, localMavenRepo)

libraryDependencies ++= List(
  "org.scorexfoundation" %% "scrypto" % "1+",
  "org.scorexfoundation" %% "scorex-core" % "1+",
  "org.scorexfoundation" %% "scorex-util" % "0+"
)

