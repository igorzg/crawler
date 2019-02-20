name := """sphere-api-crawlers"""
organization := "org.sphere"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  guice,
  ws
)
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "com.typesafe.play" %% "play-json" % "2.6.0",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.16.2-play27",
  "org.apache.httpcomponents" % "httpclient" % "4.5.7",
  "org.jsoup" % "jsoup" % "1.7.2"
)