organization := "no.scalabin.http4s"

name := "http4s-directives"

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.12.8", "2.11.12")

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-encoding",
  "utf-8"
)

val http4sVersion = "0.20.1"

libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-core"         % http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "org.http4s"    %% "http4s-blaze-server" % http4sVersion % Test,
  "org.http4s"    %% "http4s-blaze-client" % http4sVersion % Test,
  "org.scalatest" %% "scalatest"           % "3.0.7"       % Test
)

addCompilerPlugin("org.spire-math"  %% "kind-projector" % "0.9.10")

mdocIn := baseDirectory.value / "mdoc" / "docs"

mdocAutoDependency := false
micrositeCompilingDocsTool := WithMdoc
micrositeName := "http4s-directives"
micrositeDescription := "Directives for http4s"
micrositeAuthor := "scalabin-no"
micrositeOrganizationHomepage := "https://http4s-directives.scalabin.no"
micrositeDocumentationUrl := "docs/"
micrositeGithubOwner := "scalabin-no"
micrositeGithubRepo := "http4s-directives"
micrositeHighlightTheme := "tomorrow"
micrositeShareOnSocial := false
micrositeGitterChannel := false

enablePlugins(MicrositesPlugin)
