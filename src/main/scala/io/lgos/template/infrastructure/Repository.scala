package io.lgos.template.infrastructure

import cats.effect.Async
import io.getquill.doobie.DoobieContext
import io.getquill.{CompositeNamingStrategy2, NamingStrategy, PluralizedTableNames, SnakeCase}
import doobie.Transactor
import doobie.implicits.*
import doobie.free.ConnectionIO

import scala.language.implicitConversions

trait Repository[F[_]: Async] {

  def transactor: Transactor[F]

  given transact[T]: Conversion[ConnectionIO[T], F[T]] with {
    def apply(program: ConnectionIO[T]): F[T] = program.transact(transactor)
  }

  val ctx: DoobieContext.Postgres[CompositeNamingStrategy2[SnakeCase.type, PluralizedTableNames.type]] =
    DoobieContext.Postgres(NamingStrategy(SnakeCase, PluralizedTableNames))

}
