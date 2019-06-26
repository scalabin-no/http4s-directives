import microsites.MicrositesPlugin.autoImport.{micrositeCompilingDocsTool, micrositeDescription}
import sbt.CrossVersion

val http4sVersion = "0.21.0-M4"

inThisBuild(
  Seq(
    organization := "no.scalabin.http4s",
    crossScalaVersions := Seq("2.13.0", "2.12.9", "2.11.12"),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "utf-8"
    ),
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-core"         % http4sVersion,
      "org.http4s"    %% "http4s-dsl"          % http4sVersion,
      "org.http4s"    %% "http4s-blaze-server" % http4sVersion % Test,
      "org.http4s"    %% "http4s-blaze-client" % http4sVersion % Test,
      "org.scalatest" %% "scalatest"           % "3.0.8" % Test
    )
  ))

lazy val root = (project in file(".")).settings(
  name := "http4s-directives",
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary),
  scalacOptions ++= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 => Seq("-Ypartial-unification")
      case _ => Seq.empty
    })
)

lazy val mdoc = (project in file("mdoc"))
  .settings(
    micrositeCompilingDocsTool := WithMdoc,
    micrositeName := "http4s-directives",
    micrositeDescription := "Directives for http4s",
    micrositeAuthor := "scalabin-no",
    micrositeOrganizationHomepage := "https://http4s-directives.scalabin.no",
    micrositeDocumentationUrl := "docs/",
    micrositeGithubOwner := "scalabin-no",
    micrositeGithubRepo := "http4s-directives",
    micrositeHighlightTheme := "tomorrow",
    micrositeShareOnSocial := false,
    micrositeGitterChannel := false,
    mdocIn := baseDirectory.value / "docs",
    publishArtifact := false
  )
  .dependsOn(root)
  .enablePlugins(MicrositesPlugin)
