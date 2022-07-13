package io.lgos.template.user

import cats.MonadThrow
import cats.data.EitherT
import cats.effect.IO
import cats.effect.implicits.*
import cats.implicits.*
import cats.effect.Async
import io.lgos.template.common.Result.*
import UserRoutes.{RegisterRequest, UpdateUserRequest}
import io.lgos.template.common.Failure.{BadRequest, NotFound, UnexpectedError}
import UserValidation.*

trait UserService[F[_]] {

  def register(
    request: RegisterRequest
  ): Result[F, User]

  def findUser(
    userId: Long
  ): Result[F, User]

  def findUserByName(
    username: String
  ): Result[F, User]

  def deleteUser(
    userId: Long
  ): F[Unit]

  def updateUser(
    id: Long,
    user: UpdateUserRequest
  ): Result[F, User]

  def list(
    pageSize: Int,
    offset: Int
  ): Result[F, List[User]]
}

object UserService {
  def implIO(userRepository: UserRepository[IO]): UserService[IO] = impl[IO](userRepository)

  def impl[F[_]: Async](userRepository: UserRepository[F]): UserService[F] = new UserService[F] {

    private def userIdNotFound(id: Long) = NotFound(s"User with Id ${id} was not found")

    private def usernameNotFound(username: String) = NotFound(s"User with username ${username} was not found")

    override def register(
      request: RegisterRequest
    ): Result[F, User] =
      for {
        user <- UserValidation.validateUserRegister(request).toValidationResult
        result <- userRepository
          .create(user)
          .toResult
      } yield result

    override def findUser(
      userId: Long
    ): Result[F, User] =
      userRepository
        .findById(userId)
        .toResult
        .ensure(userIdNotFound(userId))(_.isDefined)
        .map(_.get)

    override def findUserByName(
      username: String
    ): Result[F, User] = userRepository
      .findByUsername(username)
      .toResult
      .ensure(usernameNotFound(username))(_.isDefined)
      .map(_.get)

    override def deleteUser(
      userId: Long
    ): F[Unit] =
      userRepository
        .delete(userId)
        .ensure(userIdNotFound(userId))(_.isDefined)
        .as(())

    override def updateUser(
      id: Long,
      user: UpdateUserRequest
    ): Result[F, User] = userRepository
      .update(
        id,
        user.email,
        user.username,
        user.firstName,
        user.lastName
      )
      .toResult
      .ensure(userIdNotFound(id))(_.isDefined)
      .map(_.get)

    override def list(
      pageSize: Int,
      offset: Int
    ): Result[F, List[User]] = userRepository.list(pageSize, offset).toResult
  }
}
