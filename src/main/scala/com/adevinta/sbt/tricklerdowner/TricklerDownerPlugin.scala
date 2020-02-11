package com.adevinta.sbt.tricklerdowner

import java.io.FileReader

import cats.syntax.either._
import io.circe.Json
import io.circe.yaml.parser
import sbt.Defaults.sbtPluginExtra
import sbt.Keys._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName
import sbt.{AutoPlugin, Def, ModuleID, Setting, _}
import scalaj.http.Http

import scala.annotation.tailrec
import scala.sys.process.Process


object TricklerDownerPlugin extends AutoPlugin {

  private type Version = String

  private val DependenciesFileName = "managed-dependencies.yml"

  private val DependenciesKey = "dependencies"

  private val DefaultEndpoint = "https://devhose.mpi-internal.com/devhose/tricklerdowner-client"

  private case class GitInfo(commit: String, repoUrl: String)

  object autoImport extends TricklerDownerKeys {

    def managedDependency(firstDep: OrganizationArtifactName): Def.Initialize[ModuleID] = Def.setting {
      val configFile = tricklerdownerConfigFile.value
      val baseDir = baseDirectory.value
      constructManagedDependencies(configFile, baseDir, Seq(firstDep))(moduleBuilder()).head
    }

    def managedDependencies(firstDep: OrganizationArtifactName,
                            otherDeps: OrganizationArtifactName*): Def.Initialize[Seq[ModuleID]] = Def.setting {
      val configFile = tricklerdownerConfigFile.value
      val baseDir = baseDirectory.value
      val allDeps = firstDep +: otherDeps
      constructManagedDependencies(configFile, baseDir, allDeps)(moduleBuilder())
    }

    def managedDependenciesWithConfig(sbtConfig: Configuration)
                                     (firstDep: OrganizationArtifactName,
                                      otherDeps: OrganizationArtifactName*): Def.Initialize[Seq[ModuleID]] = Def.setting {
      val configFile = tricklerdownerConfigFile.value
      val baseDir = baseDirectory.value
      val allDeps = firstDep +: otherDeps
      constructManagedDependencies(configFile, baseDir, allDeps)(moduleBuilder(Some(sbtConfig)))
    }

    def addManagedSbtPlugins(dependencies: OrganizationArtifactName*): Setting[Seq[ModuleID]] = {
      libraryDependencies ++= {
        val sbtV = (sbtBinaryVersion in pluginCrossBuild).value
        val scalaV = (scalaBinaryVersion in update).value
        managedDependencies(dependencies.head, dependencies.tail: _*).value.map { dependency =>
          sbtPluginExtra(dependency, sbtV, scalaV)
        }
      }
    }
  }

  import autoImport._

  override def trigger: PluginTrigger = allRequirements


  override def projectSettings: Seq[Def.Setting[_]] = Seq(

    trickledownerConfigFromRoot := true,

    tricklerdownerConfigFile := {
      if (trickledownerConfigFromRoot.value) {
        searchConfigFileIn(baseDirectory.value) / DependenciesFileName
      } else {
        baseDirectory.value / DependenciesFileName
      }
    },

    tricklerdownerEndpoint := DefaultEndpoint,

    tricklerdownerPublish := {
      implicit val logger: Logger = streams.value.log

      val result = submitEvent(
        baseDirectory.value,
        organization.value,
        artifacts = artifacts.value,
        deps = allDependencies.value,
        version = version.value,
        tricklerdownerEndpoint.value)

      result.left.foreach { error =>
        logger.error(error.message)
        throw new RuntimeException(error.message)
      }
    }
  )

  @tailrec
  private[tricklerdowner] def searchConfigFileIn(directory: File): File = {
    if (directory == null) throw new RuntimeException(s"Could not find $DependenciesFileName in any parent directory")
    if (new File(directory, DependenciesFileName).exists()) directory else searchConfigFileIn(directory.getParentFile)
  }

  private[tricklerdowner] def loadVersion(dep: OrganizationArtifactName, configFile: File, baseDir: File): Either[Error, String] = {
    for {
      file     <- existingConfigFile(configFile)
      filePath  = file.toString
      reader   <- createReader(file, filePath)
      config   <- parseConfig(reader, filePath)
      version  <- extractVersion(config, dep, filePath)
    } yield version
  }

  private def existingConfigFile(configFile: sbt.File): Either[FileNotFound, sbt.File] = {
    Either.cond(
      configFile.exists,
      configFile,
      FileNotFound(configFile.getPath))
  }

  private def createReader(file: sbt.File, filePath: String): Either[Error, FileReader] = {
    Either.catchNonFatal(new FileReader(file))
      .leftMap(ReaderError(filePath, _))
  }

  private def parseConfig(reader: FileReader, filePath: String): Either[Error, Json] = {
    parser.parse(reader)
      .leftMap(ParseError(filePath, _))
  }

  private def extractVersion(config: Json, dep: OrganizationArtifactName, filePath: String): Either[Error, String] = {
    val key = versionKey(dep)

    config.hcursor
      .downField(DependenciesKey)
      .downField(key)
      .as[String]
      .leftMap(_ => KeyNotFound(filePath, s"$DependenciesKey / $key"))
  }

  private def versionKey(org: String, artifactName: String): String = {
    s"$org:$artifactName"
  }

  private def versionKey(dep: OrganizationArtifactName): String = {
    val m = dep.%("0.0.0")
    versionKey(m.organization, m.name)
  }

  private[tricklerdowner] def submitEvent(baseDir: File,
                                          organisation: String,
                                          artifacts: Seq[Artifact],
                                          deps: Seq[ModuleID],
                                          version: String,
                                          endpoint: String)
                                         (implicit logger: Logger): Either[Error, Unit] = {

    val eventArtifacts = artifacts
      .map(a => s"$organisation:${a.name}")
      .foldLeft(Set.empty[String])({ case (acc, a) => acc + a })
      .toSeq.sorted
      .map(Json.fromString)

    val eventDeps = deps.map(d => Json.fromString(s"${d.organization}:${d.name}:${d.revision}"))

    val gitInfo = getGitInfo(baseDir).getOrElse(GitInfo("unknown", "unknown"))

    val event = Json.fromFields(Seq(
      "client" -> Json.fromString("yml"),
      "artifacts" -> Json.fromValues(eventArtifacts),
      "dependencies" -> Json.fromValues(eventDeps),
      "repository" -> Json.fromString(gitInfo.repoUrl),
      "commit" -> Json.fromString(gitInfo.commit),
      "version" -> Json.fromString(version)
    ))

    sendDevhose(endpoint, event.noSpaces)
  }

  private def sendDevhose(endpoint: String, event: String)(implicit logger: Logger): Either[Error, Unit] = {
    logger.info(s"Submitting event to $endpoint ...")
    logger.info(event)

    for {
      response <- Either.catchNonFatal {
          Http(endpoint)
            .postData(event)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .asString
        }.leftMap(HttpConnectionError(endpoint, _))

      result <- Either.cond(
        (response.code / 100) == 2,
        (),
        HttpResponseError(endpoint, response.code, response.body))
    } yield result
  }

  private def getGitInfo(baseDir: File): Either[Error, GitInfo] = {

    for {
      commit  <- run("git rev-parse HEAD", baseDir)
      repoUrl <- run("git remote get-url origin", baseDir)
    } yield GitInfo(commit = commit, repoUrl = repoUrl)
  }

  private def run(cmd: String, cwd: File): Either[Error, String] = {
    Either.catchNonFatal(Process(cmd, cwd).!!)
      .leftMap(FailedCommand(cmd, _))
      .flatMap { output =>
        output.split('\n') match {
          case Array(out) => out.asRight
          case _ => UnexpectedCommandOutput(cmd, output).asLeft
        }
      }
  }

  private def constructManagedDependencies(tricklerdownerConfigFile: File, baseDir: File, deps: Seq[OrganizationArtifactName])
                                          (moduleIdBuilder: (OrganizationArtifactName, Version) => ModuleID): Seq[ModuleID] = {
    deps.map { dep =>
      loadVersion(dep, tricklerdownerConfigFile, baseDir).map(moduleIdBuilder(dep, _)) match {
        case Left(error) =>
          throw new RuntimeException(error.message)
        case Right(module) => module
      }
    }
  }

  def moduleBuilder(maybeConfig: Option[Configuration] = None)(dep: OrganizationArtifactName, version: Version): ModuleID = {
    maybeConfig.foldLeft(dep % version) { _ % _ }
  }
}

