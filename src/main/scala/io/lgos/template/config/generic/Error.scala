package io.lgos.template.config.generic

import com.typesafe.config.Config
import cats.implicits._
import cats.data._

sealed trait ConfigError extends Throwable

case class LoadingError(
  underlying: Throwable)
  extends ConfigError {
  def message = s"Error while loading config: ${underlying.getMessage}"
  override def toString = message
}

case class ParsingError(
  config: String,
  path: String,
  underlying: Throwable)
  extends ConfigError {
  def message = s"Error while parsing ${path}: ${underlying} on ${config}"
  override def toString = message
}

object ParsingError {

  def catchNonFatal[T](
    config: Config,
    path: String
  )(
    f: => T
  ): Either[ParsingError, T] =
    Either
      .catchNonFatal {
        f
      }
      .left
      .map { error =>
        ParsingError(config.toString, path, error)
      }
}
