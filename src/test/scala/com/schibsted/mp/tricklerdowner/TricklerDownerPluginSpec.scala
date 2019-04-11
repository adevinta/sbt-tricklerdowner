package com.schibsted.mp.tricklerdowner

import java.io.{File, PrintWriter}
import java.net.UnknownHostException
import java.nio.file.Files

import scala.sys.process.Process
import TricklerDownerPlugin.{loadVersion, submitEvent, searchConfigFileIn}
import cats.Id
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, ok, post, postRequestedFor, urlEqualTo, aResponse}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.circe.Json
import org.scalatest.{FlatSpec, Inside, Matchers}
import sbt.librarymanagement.{Artifact, DependencyBuilders, ModuleID}
import sbt.util.Logger

class TricklerDownerPluginSpec extends FlatSpec with Matchers with Inside with DependencyBuilders {

  import TricklerDownerPluginSpec._

  "loadVersion" should "load the version from the config file" in {
    val tempDir = Files.createTempDirectory("test").toFile
    val configFile = createFile(tempDir, "managed-dependencies.yml", """
          |dependencies:
          |  org:art: 0.1.0
      """.stripMargin)

    loadVersion("org" % "art", configFile, configFile.getParentFile) shouldBe Right("0.1.0")
  }

  it should "fail to load the version when there is no config file" in {
    val tempDir = Files.createTempDirectory("test").toFile
    val configFile = new File(tempDir, "notfound.yml")

    val result = loadVersion("org" % "art", configFile, configFile.getParentFile)
    result shouldBe Left(FileNotFound(configFile.toString))
  }

  it should "fail to load the version when the reader fails to read the config file" in {
    val tempDir = Files.createTempDirectory("test").toFile
    val configFile = createFile(tempDir, "managed-dependencies.yml", """
        |dependencies:
        |  org:art: 0.1.0
      """.stripMargin)
    configFile.setReadable(false)

    inside(loadVersion("org" % "art", configFile, configFile.getParentFile)) {
      case Left(ReaderError(path, cause)) =>
        path shouldBe configFile.toString
    }
  }

  it should "fail to load the version when the config file fails to parse" in {
    val tempDir = Files.createTempDirectory("test").toFile
    val configFile = createFile(tempDir, "managed-dependencies.yml", """
        |dependencies: [1,2,3}
      """.stripMargin)

    inside(loadVersion("org" % "art", configFile, configFile.getParentFile)) {
      case Left(ParseError(path, cause)) =>
        path shouldBe configFile.toString
    }
  }

  it should "fail to load the version when it is not defined in the config file" in {
    val tempDir = Files.createTempDirectory("test").toFile
    val configFile = createFile(tempDir, "managed-dependencies.yml", """
        |dependencies:
        |  org:art: 0.1.0
      """.stripMargin)

    val result = loadVersion("org" % "notfound", configFile, configFile.getParentFile)
    result shouldBe Left(KeyNotFound(configFile.toString, s"dependencies / org:notfound"))
  }

  "submitEvent" should "submit an event successfully" in {
    withGitRepo { (tempDir, repoUrl, commit) =>
      withWireMock { server =>

        server.stubFor(
          post(urlEqualTo("/client"))
            .willReturn(ok())
        )

        implicit val logger: Logger = Logger.Null

        val endpoint = s"http://localhost:${server.port}/client"

        submitEvent(tempDir, "org", artifacts, deps, "1.2.3", endpoint) shouldBe Right(())

        server.verify(
          postRequestedFor(urlEqualTo("/client"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(expectedEvent(repoUrl, commit), true, false))
        )
      }
    }
  }

  it should "fail to submit an event when there is a connection error" in {
    withGitRepo { (tempDir, repoUrl, commit) =>
      implicit val logger: Logger = Logger.Null

      val endpoint = s"http://_xyz_:12345/client"

      inside(submitEvent(tempDir, "org", artifacts, deps, "1.2.3", endpoint)) {
        case Left(HttpConnectionError(ep, cause)) =>
          ep shouldBe endpoint
          cause shouldBe a[UnknownHostException]
          cause.getMessage should include ("_xyz_")
      }
    }
  }

  it should "handle an HTTP error" in {
    withGitRepo { (tempDir, repoUrl, commit) =>
      withWireMock { server =>

        server.stubFor(
          post(urlEqualTo("/client"))
            .willReturn(aResponse().withStatus(404).withBody("error"))
        )

        implicit val logger: Logger = Logger.Null

        val endpoint = s"http://localhost:${server.port}/client"

        inside(submitEvent(tempDir, "org", artifacts, deps, "1.2.3", endpoint)) {
          case Left(HttpResponseError(ep, code, body)) =>
            ep shouldBe endpoint
            code shouldBe 404
            body shouldBe "error"
        }
      }
    }
  }

  it should "search in parent directories for the managed dependency yml" in {
    val rootPath = Files.createTempDirectory("test").toFile
    val manageDepFileName = "managed-dependencies.yml"
    createFile(rootPath, manageDepFileName, """
                                             |dependencies:
                                             |  org:art: 0.1.0
                                           """.stripMargin)

    val firstLevel = Files.createTempDirectory(rootPath.toPath, "firstLevel").toFile
    val secondLevel = Files.createTempDirectory(firstLevel.toPath, "secondLevel").toFile
    searchConfigFileIn(secondLevel) shouldBe rootPath
  }

  it should "return at first directory if  managed dependency yml is present there" in {
    val rootPath = Files.createTempDirectory("test").toFile.toPath
    val firstLevel = Files.createTempDirectory(rootPath, "firstLevel").toFile
    val secondLevel = Files.createTempDirectory(firstLevel.toPath, "secondLevel").toFile
    val manageDepFileName = "managed-dependencies.yml"
    createFile(secondLevel, manageDepFileName, """
                                             |dependencies:
                                             |  org:art: 0.1.0
                                           """.stripMargin)

    searchConfigFileIn(secondLevel) shouldBe secondLevel
  }

  it should "return null if managed-dependencies was not found in any parent directory" in {
    val rootPath = Files.createTempDirectory("test").toFile.toPath
    val firstLevel = Files.createTempDirectory(rootPath, "firstLevel").toFile
    val secondLevel = Files.createTempDirectory(firstLevel.toPath, "secondLevel").toFile

    the[RuntimeException] thrownBy searchConfigFileIn(secondLevel) should have message
      "Could not find managed-dependencies.yml in any parent directory"
  }


}

object TricklerDownerPluginSpec {
  type RepoUrl = Id[String]
  type Commit = Id[String]

  private val artifacts = Seq(
    Artifact("art1"),
    Artifact("art2"),
    Artifact("art2"))

  private val deps = Seq(
    ModuleID("org1", "lib1", "0.1.0"),
    ModuleID("org2", "lib2", "0.2.0"))

  private def expectedEvent(repoUrl: String, commit: String) = Json.fromFields(Seq(
    "client" -> Json.fromString("yml"),
    "artifacts" -> Json.fromValues(Seq("org:art1", "org:art2").map(Json.fromString)),
    "dependencies" -> Json.fromValues(Seq("org1:lib1:0.1.0", "org2:lib2:0.2.0").map(Json.fromString)),
    "repository" -> Json.fromString(repoUrl),
    "commit" -> Json.fromString(commit),
    "version" -> Json.fromString("1.2.3")
  )).noSpaces

  private def createFile(baseDir: File, name: String, content: String): File = {
    val file = new File(baseDir, name)
    new PrintWriter(file) {
      try {
        write(content)
      }
      finally {
        close()
      }
    }
    file
  }

  private def withGitRepo(test: (File, RepoUrl, Commit) => Any): Any = {
    val tempDir = Files.createTempDirectory("test").toFile

    createFile(tempDir, "test.txt", "test")

    Process("git init", tempDir).!

    val repoUrl: RepoUrl = "git@localhost:test.git"
    Process(s"git remote add origin $repoUrl", tempDir).!

    Process("git add test.txt", tempDir).!
    Process("git commit -m 'testing'", tempDir).!
    val commit: Commit = Process("git rev-parse HEAD", tempDir).!!.trim()

    test(tempDir, repoUrl, commit)
  }

  private def withWireMock(test: WireMockServer => Any): Any = {
    val serverConfig = WireMockConfiguration.wireMockConfig().dynamicPort()
    val server = new WireMockServer(serverConfig)
    server.start()
    try {
      test(server)
    }
    finally {
      server.stop()
    }
  }
}
