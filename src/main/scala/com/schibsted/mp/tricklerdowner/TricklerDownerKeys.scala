package com.schibsted.mp.tricklerdowner

import sbt.{File, settingKey, taskKey}

trait TricklerDownerKeys {

  val tricklerdownerConfigFile = settingKey[File]("File containing the config to use for the managed dependencies")

  val trickledownerConfigFromRoot = settingKey[Boolean]("If this is 'true' will take configuration file " +
    "from submodule directory. Otherwise it will get it from project root folder")

  val tricklerdownerEndpoint = settingKey[String]("Devhose endpoint for Tricklerdowner")

  val tricklerdownerPublish = taskKey[Unit]("Submit an event with version information to the TricklerDowner endpoint")


}
