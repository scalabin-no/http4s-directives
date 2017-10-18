val nexusRepos = "https://shiplog.jfrog.io/shiplog/"

publishTo := {
  if (isSnapshot.value) {
    Some("Shiplog Snapshots" at nexusRepos + "libs-snapshot-local/")
  } else {
    Some("Shiplog Releases" at nexusRepos + "libs-release-local/")
  }
}

credentials += Credentials(Path.userHome / ".sbt" / "artifactory.credentials")

startYear := Some(2017)