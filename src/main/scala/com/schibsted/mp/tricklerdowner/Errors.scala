package com.schibsted.mp.tricklerdowner

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
