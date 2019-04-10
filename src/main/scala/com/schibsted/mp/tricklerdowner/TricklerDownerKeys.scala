package com.schibsted.mp.tricklerdowner

import sbt.{File, settingKey, taskKey}

trait TricklerDownerKeys {

  val tricklerdownerConfigFile = settingKey[File]("File containing the config to use for the managed dependencies")

  val tricklerdownerEndpoint = settingKey[String]("Devhose endpoint for Tricklerdowner")

  val tricklerdownerPublish = taskKey[Unit]("Submit an event with version information to the TricklerDowner endpoint")

  val tricklerdownerDirectoryConfigFile = settingKey[File]("alternative directory to find managed-dependencies.yml " +
    "file. This is usefull for multimodule projects when you dont want to repeat same yml file.")


}
