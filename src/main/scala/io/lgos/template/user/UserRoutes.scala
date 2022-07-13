package io.lgos.template.user

import cats.data.NonEmptyList
import cats.effect.{Async, IO}
import cats.implicits.*
import io.lgos.template.common.Failure.*
import io.lgos.template.infrastructure.Json.*
import io.lgos.template.util.ServerEndpoints
import io.lgos.template.api.Http
import io.lgos.template.common.Failure
import sttp.model.StatusCode

class UserRoutes[F[_]: Async](
  userService: UserService[F]) {
  import Http.*
  import UserRoutes.*

  private val UsersPath = "users"

  private val registerUserEndpoint = endpoint.post
    .in(UsersPath)
    .in(jsonBody[RegisterRequest])
    .out(statusCode(StatusCode.Created).and(jsonBody[AuthenticatedUserResponse]))
    .errorOut(
      oneOf[Failure](
        defaultUnauthorized,
        defaultValidationFailed,
        defaultConflict
      )
    )
    .serverLogic { data =>
      (
        for {
          user <- userService.register(data)
        } yield user.toAuthenticatedResponse
      ).value
    }

  private val getUserEndpoint = endpoint.get
    .in(UsersPath / path[Long].name("userId"))
    .out(jsonBody[UserResponse])
    .errorOut(
      oneOf[Failure](
        defaultUnauthorized,
        defaultValidationFailed,
        defaultNotFound
      )
    )
    .serverLogic(id =>
      (for {
        user <- userService.findUser(id)
      } yield user.toResponse).value
    )

  private val listUsersEndpoint = endpoint.get
    .in(UsersPath)
    .in(query[Int]("pageSize").default(100).and(query[Int]("offset").default(0)))
    .out(jsonBody[List[UserResponse]])
    .errorOut(
      oneOf[Failure](
        defaultUnauthorized,
        defaultValidationFailed,
        defaultNotFound
      )
    )
    .serverLogic { case (pageSize, offset) =>
      (for {
        user <- userService.list(pageSize, offset)
      } yield user.map(_.toResponse)).value
    }

  private val updateUserEndpoint = endpoint.put
    .in(UsersPath / path[Long].name("userId"))
    .in(jsonBody[UpdateUserRequest])
    .out(jsonBody[UserResponse])
    .errorOut(
      oneOf[Failure](
        defaultUnauthorized,
        defaultValidationFailed,
        defaultConflict,
        defaultNotFound
      )
    )
    .serverLogic { case (id, body) =>
      (for {
        user <- userService.updateUser(id, body)
      } yield user.toResponse).value
    }

  private val deleteUserEndpoint = endpoint.delete
    .in(UsersPath / path[Long].name("userId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(
      oneOf[Failure](
        defaultUnauthorized,
        defaultValidationFailed,
        defaultNotFound
      )
    )
    .serverLogic { id =>
      (for {
        user <- userService.deleteUser(id)
      } yield user).toOut
    }

  val endpoints: ServerEndpoints[F] =
    NonEmptyList
      .of(
        registerUserEndpoint,
        getUserEndpoint,
        updateUserEndpoint,
        deleteUserEndpoint,
        listUsersEndpoint
      )
      .map(_.tag("user"))
}

object UserRoutes {

  def implIO(
    userService: UserService[IO]
  ) = new UserRoutes[IO](userService)

  final case class LoginRequest(
    username: String,
    password: String)

  final case class RegisterRequest(
    username: String,
    email: String,
    password: String,
    firstName: Option[String] = None,
    lastName: Option[String] = None)

  case class AuthenticatedUserResponse(
    id: Long,
    email: String,
    username: String,
    token: String,
    firstName: Option[String],
    lastName: Option[String])

  case class UpdateUserRequest(
    email: String,
    username: String,
    firstName: Option[String],
    lastName: Option[String])

  case class UserResponse(
    id: Long,
    email: String,
    username: String,
    firstName: Option[String],
    lastName: Option[String])
}
