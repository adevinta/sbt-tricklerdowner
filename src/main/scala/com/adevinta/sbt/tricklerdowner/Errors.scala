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

sealed trait Error { def message: String }

case class FileNotFound(path: String) extends Error {
  val message = s"File not found: $path"
}

case class ReaderError(path: String, cause: Throwable) extends Error {
  val message = s"Error opening file for read: $path\n$cause"
}

case class WriterError(path: String, cause: Throwable) extends Error {
  val message = s"Error opening file for write: $path\n$cause"
}

case class WriteError(path: String, cause: Throwable) extends Error {
  val message = s"Error writing to the configuration file: $path\n$cause"
}

case class ParseError(path: String, cause: Throwable) extends Error {
  val message = s"Failed to parse the configuration file: $path\n$cause"
}

case class KeyNotFound(path: String, key: String) extends Error {
  val message = s"Failed to find '$key' in the configuration file: $path"
}

case class FailedCommand(cmd: String, cause: Throwable) extends Error {
  val message = s"Failed to run command '$cmd': $cause"
}

case class UnexpectedCommandOutput(cmd: String, output: String) extends Error {
  val message = s"Unexpected output for command '$cmd': $output"
}

case class HttpResponseError(endpoint: String, code: Int, body: String) extends Error {
  val message = s"HTTP response error $code from $endpoint: $body"
}

case class HttpConnectionError(endpoint: String, cause: Throwable) extends Error {
  val message = s"HTTP connection error to $endpoint: $cause"
}
