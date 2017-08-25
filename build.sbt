organization := "net.hamnaberg.http4s"

name := "http4s-directives"

scalaVersion := "2.12.3"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-encoding", "utf-8"
)

libraryDependencies += "org.http4s" %% "http4s-core" % "0.17.0-RC2"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.17.0-RC2"

libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.17.0-RC2" % "test"

