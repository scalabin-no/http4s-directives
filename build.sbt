organization := "net.hamnaberg.http4s"

name := "http4s-directives"

scalaVersion := "2.12.1"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-encoding", "utf-8"
)

libraryDependencies += "org.http4s" %% "http4s-core" % "0.15.3a"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.15.3a"

libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.15.3a" % "test"

