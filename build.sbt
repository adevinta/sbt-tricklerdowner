import sbt.URL

inThisBuild(Seq(
  scalaVersion := "2.12.5",
  crossScalaVersions := Seq("2.11.12", "2.12.5"),
  organization := "com.adevinta",
  description := "SBT plugin that integrates the build with TricklerDowner managed dependencies by Yaml",

  sbtPlugin := true
))

lazy val defaultSettings = Defaults.coreDefaultSettings ++ Seq(
  resolvers ++= Seq(
    Resolver.jcenterRepo,
    Resolver.mavenLocal
  )
)

lazy val artifactorySettings = {
  def getEnv(name: String): String = {
    val value = System.getenv(name)
    if (value == null) {
      println("You are missing the environment variables needed to access Artifactory.")
      println("Please follow this guide to configure your machine properly:")
      println("\nhttps://docs.mpi-internal.com/unicron/docs-zeus-migration-guide/setup-laptop/\n")
      sys.exit(-1)
    }
    value
  }

  val artifactoryContext = getEnv("ARTIFACTORY_CONTEXT")
  val artifactoryUser = getEnv("ARTIFACTORY_USER")
  val artifactoryPass = getEnv("ARTIFACTORY_PWD")

  Seq(
    resolvers := Seq("Artifactory Release Plugins" at s"$artifactoryContext/libs-release"),
    credentials := Seq(Credentials("Artifactory Realm", new URL(artifactoryContext).getAuthority, artifactoryUser, artifactoryPass)),
    publishTo := {
      val repository = if (isSnapshot.value) "libs-snapshot-local;build.timestamp=" + java.time.Instant.now().toEpochMilli else "libs-release-local"
      Some("Artifactory Realm for Publishing" at s"$artifactoryContext/$repository/")
    },
    dynverSonatypeSnapshots := true
  )
}

lazy val bintraySettings = Seq(
  publishMavenStyle := false,
  bintrayOrganization := Some("adevinta-unicron"),
  bintrayRepository := "sbt-plugins",
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  dynverSonatypeSnapshots := false
)

val publishRepository = sys.props.get("publish.repository")

val publishSettings = {
  publishRepository
    .filter(_.toLowerCase == "bintray")
    .fold[Seq[Setting[_]]](artifactorySettings)(_ => bintraySettings)
}

val pluginsToDisable = {
  publishRepository
    .filter(_.toLowerCase == "bintray")
    .fold[Seq[AutoPlugin]](Seq(BintrayPlugin))(_ => Seq.empty)
}

lazy val root = Project(id = "sbt-tricklerdowner", base = file("."))
  .disablePlugins(pluginsToDisable: _*)
  .settings(defaultSettings)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-yaml" % "0.7.0",
      "org.scalaj" %% "scalaj-http" % "2.4.0",

      "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test",
      "com.github.tomakehurst" % "wiremock" % "2.18.0" % "test"
    )
  )
