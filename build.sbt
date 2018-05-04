organization := "no.scalabin.http4s"

name := "http4s-directives"

scalaVersion := "2.12.5"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-encoding", "utf-8"
)

val http4sVersion = "0.18.10"

libraryDependencies += "org.http4s" %% "http4s-core" % http4sVersion
libraryDependencies += "org.typelevel" %% "cats-effect" % "0.10.1"
libraryDependencies += "org.http4s" %% "http4s-dsl" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % http4sVersion % "test"


overridePublishBothSettings
