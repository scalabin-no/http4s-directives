val http4sVersion = "1.0.0-M19"

inThisBuild(
  Seq(
    organization := "no.scalabin.http4s",
    crossScalaVersions := Seq("2.13.5", "2.12.13"),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "utf-8",
      "-language:higherKinds"
    ),
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-core"         % http4sVersion,
      "org.http4s"    %% "http4s-dsl"          % http4sVersion,
      "org.http4s"    %% "http4s-blaze-server" % http4sVersion % Test,
      "org.http4s"    %% "http4s-blaze-client" % http4sVersion % Test,
      "org.scalatest" %% "scalatest"           % "3.2.6"       % Test
    )
  )
)

lazy val root = (project in file(".")).settings(
  name := "http4s-directives",
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full),
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 12 => Seq("-Ypartial-unification")
    case _                       => Seq.empty
  })
)

lazy val mdoc = (project in file("mdoc"))
  .settings(
   // micrositeCompilingDocsTool := WithMdoc,
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
