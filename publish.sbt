val nexusRepos = "https://shiplog.jfrog.io/shiplog/"

publishTo := {
  if (isSnapshot.value) {
    Some("Shiplog Snapshots" at nexusRepos + "libs-snapshot-local/")
  } else {
    Some("Shiplog Releases" at nexusRepos + "libs-release-local/")
  }
}

releaseCrossBuild := true

pomIncludeRepository := { x => false }

fork in run := true

javaOptions += "-Xmx2G"

packageOptions <+= (name, version, organization) map {
  (title, version, vendor) =>
    Package.ManifestAttributes(
      "Created-By" -> "Scala Build Tool",
      "Built-By" -> System.getProperty("user.name"),
      "Build-Jdk" -> System.getProperty("java.version"),
      "Specification-Title" -> title,
      "Specification-Version" -> version,
      "Specification-Vendor" -> vendor,
      "Implementation-Title" -> title,
      "Implementation-Version" -> version,
      "Implementation-Vendor-Id" -> vendor,
      "Implementation-Vendor" -> vendor
    )
}

credentials += Credentials(Path.userHome / ".sbt" / "artifactory.credentials")

homepage := Some(url("http://github.com/shiplog/directives4s"))

startYear := Some(2017)