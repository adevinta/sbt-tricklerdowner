/*
 * Copyright 2020 Adevinta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adevinta.sbt.tricklerdowner

import sbt.{File, settingKey, taskKey}

trait TricklerDownerKeys {

  val tricklerdownerConfigFile = settingKey[File]("File containing the config to use for the managed dependencies")

  val trickledownerConfigFromRoot = settingKey[Boolean]("If this is 'true' will take configuration file " +
    "from submodule directory. Otherwise it will get it from project root folder")

  val tricklerdownerEndpoint = settingKey[String]("Devhose endpoint for Tricklerdowner")

  val tricklerdownerPublish = taskKey[Unit]("Submit an event with version information to the TricklerDowner endpoint")


}
