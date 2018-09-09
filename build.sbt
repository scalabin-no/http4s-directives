organization := "no.scalabin.http4s"

name := "http4s-directives"

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.12.6", "2.11.12")

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-encoding",
  "utf-8"
)

val http4sVersion = "0.19.0-M2"

libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-core"         % http4sVersion,
  "org.typelevel" %% "cats-effect"         % "1.0.0",
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "org.http4s"    %% "http4s-blaze-server" % http4sVersion % Test,
  "org.http4s"    %% "http4s-blaze-client" % http4sVersion % Test,
  "org.scalatest" %% "scalatest"           % "3.0.5"       % Test
)
