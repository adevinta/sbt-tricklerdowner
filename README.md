[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.adevinta/sbt-tricklerdowner/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.adevinta/sbt-tricklerdowner)
[![Travis (.com)](https://img.shields.io/travis/com/adevinta/sbt-tricklerdowner)](https://travis-ci.com/github/adevinta/sbt-tricklerdowner/builds)

# SBT TricklerDowner plugin

This SBT plugin integrates the build with TricklerDowner managed dependencies by Yaml.

## Enable the SBT plugin in your projects

To enable this plugin in SBT, you just need to add a line to the `plugins.sbt` file under the `project` folder:

```scala
addSbtPlugin("com.github.adevinta" % "sbt-tricklerdowner" % "<version>")
```

Please make sure you use the proper version.

## Setup for projects consuming dependencies managed with TricklerDowner

A part from enabling the SBT plugin, you will need to include/tune the following four files in order to use TricklerDowner managed dependencies from SBT.

### .sch-bots.yml

This is the file used by the skynet bots to retrieve configuration for the repository.

First of all you will need to enable TricklerDowner like:

```yaml
TricklerDowner:
  enabled: true
```

alternatively you can skip this step by just running the following command in Slack:

```
/skynet trickler-downer enable <org>/<repository>
```

### managed-dependencies.yml

This Yaml file will include the current version to use for all the dependencies managed by TricklerDowner.

Please, make sure you include all the dependencies that will be managed by TricklerDowner here:

```yaml
dependencies:
  com.github.xyz:lib1: "1.0.0"
  com.github.xyz:lib2: "2.0.0"
```

The dependencies key is composed by the `organization` and the `artifact ID` separated by a colon.

To avoid problems with the yaml parsing, make sure there is an space between the key and the version value,
 and that the version value is between quotes.

### build.sbt

To define dependencies which versions will be extracted from the `managed-dependencies.yml` file from `build.sbt`,
you can use `managedDependencies` like:

```scala
libraryDependencies ++= managedDependencies(
  "com.github.xyz" %% "lib1",
  "com.github.xyz" %% "lib2"
).value
```

You can also mix non managed and managed like:
```scala
libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.4.0"
) ++ managedDependencies(
  "com.github.xyz" %% "lib1",
  "com.github.xyz" %% "lib2"
).value
```

### .travis.yaml

You will need to execute the `tricklerdownerPublish` task from travis to notify Skynet about the new artifacts and version
 as well as the dependency graph for the project.
 
Depending on the process adopted for releasing, the task could be launched from different places/moments. Just as an example,
this is how you would do it during merges to master:

```yaml
deploy:
  skip_cleanup: true
  provider: script
  script: "sbt publish tricklerdownerPublish"
  on:
    branch: master
```

## Setup for projects providing dependencies to other projects (ex. libraries)

In the case of projects consumed by other projects, but that don't require to be updated when other projects get released,
you still need to follow some of the previous steps, which are:

- To enable the SBT plugin for TricklerDowner.
- To publish the project details as explained before for `.travis.yml`.
