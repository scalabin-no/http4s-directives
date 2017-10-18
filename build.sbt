organization := "net.hamnaberg.http4s"

name := "http4s-directives"

scalaVersion := "2.12.3"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-encoding", "utf-8"
)

val http4sVersion = "0.18.0-M4"

libraryDependencies += "org.http4s" %% "http4s-core" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-dsl" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % http4sVersion % "test"

