package io.lgos.template.user

import cats.effect.{Async, IO}
import cats.effect.kernel.Async
import doobie.Transactor
import doobie.implicits.*
import doobie.free.ConnectionIO
import io.getquill.doobie.DoobieContext
import io.getquill.*
import io.lgos.template.infrastructure.Repository

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

trait UserRepository[F[_]] {
  def create(user: User): F[User]

  def update(
    id: Long,
    email: String,
    username: String,
    firstName: Option[String],
    lastName: Option[String]
  ): F[Option[User]]

  def findById(userId: Long): F[Option[User]]

  def findByUsername(username: String): F[Option[User]]

  def delete(userId: Long): F[Option[User]]

  def list(
    pageSize: Int,
    offset: Int
  ): F[List[User]]
}

object UserRepository {

  /**
    * Workaround for macwire not working properly with evidence parameters in scala3
    */
  def implIO(xa: Transactor[IO]): UserRepository[IO] = impl[IO](xa)

  def impl[F[_]: Async](
    xa: Transactor[F]
  ): UserRepository[F] =
    new UserRepository[F] with Repository[F] {

      import ctx.*

      val transactor: Transactor[F] = xa

      inline def users: Quoted[EntityQuery[User]] = quote(query[User])

      def create(user: User): F[User] =
        run {
          users
            .insertValue(lift(user))
            .returningGenerated(_.id)
        }.map(id => user.copy(id = id))

      def update(
        id: Long,
        email: String,
        username: String,
        firstName: Option[String],
        lastName: Option[String]
      ): F[Option[User]] = run {
        users
          .filter(_.id == lift(id))
          .update(
            _.email -> lift(email),
            _.username -> lift(username),
            _.firstName -> lift(firstName),
            _.lastName -> lift(lastName)
          )
          .returningMany(x => x)
      }.map(_.headOption)

      def findById(userId: Long): F[Option[User]] = run {
        users
          .filter(_.id == lift(userId))
      }.map(_.headOption)

      def delete(userId: Long): F[Option[User]] =
        run(users.filter(_.id == lift(userId)).delete.returningMany(x => x)).map(_.headOption)

      def findByUsername(username: String): F[Option[User]] = run {
        users
          .filter(_.username == lift(username))
      }.map(_.headOption)

      def list(
        pageSize: Int,
        offset: Int
      ): F[List[User]] =
        run(users).map(_.toList)
    }
}
