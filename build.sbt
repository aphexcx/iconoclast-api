name := """play-scala"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  specs2 % Test,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

def IN_DOCKER: Boolean = (!System.getProperty("os.name").contains("Mac OS X"))

//fork in run := !IN_DOCKER
fork in run := false


libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.9"
