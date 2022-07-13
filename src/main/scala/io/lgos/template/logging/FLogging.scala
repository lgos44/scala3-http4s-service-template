package io.lgos.template.logging

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object FLogging {
  extension [F[_]: Sync](logger: Logger) {
    def debugF(
      message: String
    ): F[Unit] =
      Sync[F].delay(logger.debug(message))

    def debugF(
      message: String,
      cause: Throwable
    ): F[Unit] =
      Sync[F].delay(logger.debug(message, cause))

    def infoF(
      message: String
    ): F[Unit] =
      Sync[F].delay(logger.info(message))

    def infoF(
      message: String,
      cause: Throwable
    ): F[Unit] =
      Sync[F].delay(logger.info(message, cause))

    def warnF(
      message: String
    ): F[Unit] =
      Sync[F].delay(logger.warn(message))

    def warnF(
      message: String,
      throwable: Option[Throwable] = None
    ): F[Unit] =
      throwable.fold {
        Sync[F].delay(logger.warn(message))
      }(throwable => Sync[F].delay(logger.warn(message, throwable)))

    def errorF(
      message: String,
      throwable: Option[Throwable] = None
    ): F[Unit] =
      throwable.fold {
        Sync[F].delay(logger.error(message))
      }(throwable => Sync[F].delay(logger.error(message, throwable)))

    def errorF(
      message: String,
      throwable: Throwable
    ): F[Unit] =
      Sync[F].delay(logger.error(message, throwable))
  }
}
