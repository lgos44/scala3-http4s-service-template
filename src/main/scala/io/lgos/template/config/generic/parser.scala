package io.lgos.template.config.generic

import com.typesafe.config.ConfigException
import com.typesafe.config.{Config, ConfigFactory}
import cats.implicits.*
import cats.data.*
import cats.ApplicativeError
import ConfigDecoder.DecodeResult
import parser.decode

object parser {

  final def decode[A](
    load: => Config,
    path: Option[String] = None
  )(using
    decoder: ConfigDecoder[A]
  ): DecodeResult[A] =
    Either
      .catchNonFatal {
        val config = load
        path
          .fold(config) { p =>
            if (config.hasPath(p)) config.getConfig(p)
            else throw new ConfigException.Missing(p)
          }
          .root
      }
      .leftMap { error =>
        LoadingError(error)
      }
      .flatMap { cfg =>
        decoder.decode(cfg.toConfig, "")
      }

  final def decode[A: ConfigDecoder]: DecodeResult[A] =
    decode(ConfigFactory.load())

  final def decodeUnsafe[A: ConfigDecoder]: A =
    decode(ConfigFactory.load()) match
      case Right(value) => value
      case Left(error) => throw error

  final def decode[A: ConfigDecoder](path: String): DecodeResult[A] =
    decode(ConfigFactory.load(), path.some)

  final def decodeF[F[_], A: ConfigDecoder]()(using ev: ApplicativeError[F, Throwable]): F[A] =
    decode[A].leftWiden[Throwable].liftTo[F]
}
