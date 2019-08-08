import java.net.URL

inThisBuild(Seq(
  scalaVersion := "2.12.5",
  crossScalaVersions := Seq("2.11.12", "2.12.5"),
  organization := "com.schibsted.mp",

  sbtPlugin := true
))

lazy val defaultSettings = Defaults.coreDefaultSettings ++ Seq(
  resolvers ++= Seq(
    Resolver.jcenterRepo,
    Resolver.mavenLocal
  )
)

val artifactoryUrl = System.getenv("ARTIFACTORY_CONTEXT")

lazy val artifactorySettings = Seq(
  resolvers += "Artifactory Realm Libs" at s"$artifactoryUrl/libs-release-local/",
  credentials += Credentials("Artifactory Realm",
    new URL(artifactoryUrl).getHost,
    System.getenv("ARTIFACTORY_USER"),
    System.getenv("ARTIFACTORY_PWD")),
  publishTo := {
    val repository = if (isSnapshot.value) "libs-snapshot-local" else "libs-release-local"
    Some("Artifactory Realm for Publish" at s"$artifactoryUrl/$repository/")
  })


lazy val root = Project(id = "sbt-tricklerdowner", base = file("."))
  .settings(defaultSettings, artifactorySettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-yaml" % "0.7.0",
      "org.scalaj" %% "scalaj-http" % "2.4.0",

      "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test",
      "com.github.tomakehurst" % "wiremock" % "2.18.0" % "test"
    )
  )
