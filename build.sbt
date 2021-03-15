import sbt.URL

inThisBuild(Seq(

  name := "sbt-tricklerdowner",
  organization := "com.github.adevinta.unicron",
  description := "SBT plugin that integrates the build with TricklerDowner managed dependencies by Yaml",

  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url(s"https://github.com/adevinta/${name.value}")),
  developers := List(Developer("cre-team", "CRE Team", "gp.gt.cre@adevinta.com", url("https://github.com/orgs/adevinta/teams/cre"))),
  scmInfo := Some(ScmInfo(url(s"https://github.com/adevinta/${name.value}"), s"scm:git:git@github.com:adevinta/${name.value}.git")),

  organizationName := "Adevinta",
  startYear := Some(2020),

  usePgpKeyHex("E362921A4CE8BD97916B06CEC6DDC7B1869C9349"),

  dynverSonatypeSnapshots := true,

  scalaVersion := "2.12.12",
  crossScalaVersions := Seq("2.11.12", "2.12.12"),
  sbtPlugin.withRank(KeyRanks.Invisible) := true,
))

lazy val root = Project(id = "sbt-tricklerdowner", base = file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    publishTo := sonatypePublishToBundle.value,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-yaml" % "0.7.0",
      "org.scalaj" %% "scalaj-http" % "2.4.0",

      "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test",
      "com.github.tomakehurst" % "wiremock" % "2.18.0" % "test"
    )
  )
