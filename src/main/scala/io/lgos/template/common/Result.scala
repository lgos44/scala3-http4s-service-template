package io.lgos.template.common

import cats.{Applicative, MonadThrow}
import cats.data.EitherT
import cats.effect.Async
import cats.implicits.*
import Failure.{Conflict, UnexpectedError}
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import org.postgresql.util.PSQLException

object Result {

  type Result[F[_], T] = EitherT[F, Failure, T]

  extension [F[_]: Async: MonadThrow, T](effect: F[T]) {
    def toResult: Result[F, T] = effect.attemptT
      .leftMap { (t: Throwable) =>
        t match {
          case ex: PSQLException if Option(ex.getServerErrorMessage.getSQLState).contains(UNIQUE_VIOLATION.value) =>
            Conflict(ex.getServerErrorMessage.getDetail)
          case _ =>
            UnexpectedError(t.getMessage)
        }
      }
  }

  def pure[F[_]: Async, T](
    t: T
  ): Result[F, T] = EitherT.pure[F, Failure](t)
}
