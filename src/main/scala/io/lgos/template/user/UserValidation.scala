package io.lgos.template.user

import cats.data.ValidatedNec
import cats.effect.Async
import cats.implicits.*
import io.lgos.template.common.Failure.{InvalidField, ValidationFailed}
import UserRoutes.RegisterRequest
import cats.data.EitherT
import io.lgos.template.common.Result.*
import io.lgos.template.user.UserRoutes.RegisterRequest

object UserValidation {
  private val emailRegex =
    """^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  private val usernameRegex = """^[a-zA-Z0-9]([._-](?![._-])|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$""".r

  def validateEmail(email: String): ValidatedNec[InvalidField, String] =
    if (emailRegex.findFirstMatchIn(email).isDefined) email.validNec
    else InvalidField("email", email, "Is not a valid email.").invalidNec

  def validateUsername(username: String): ValidatedNec[InvalidField, String] =
    if (usernameRegex.findFirstMatchIn(username).isDefined) username.validNec
    else InvalidField("username", username, "Username does not match requirements.").invalidNec

  def validatePassword(password: String): ValidatedNec[InvalidField, String] =
    if (password.length >= 8) password.validNec
    else InvalidField("password", password, "Password too short").invalidNec

  def validateUserRegister(req: RegisterRequest): ValidatedNec[InvalidField, User] =
    (validateUsername(req.username), validateEmail(req.email), validatePassword(req.password))
      .mapN(User(0L, _, _, _, None, None))

  extension [F[_]: Async, T](validated: ValidatedNec[InvalidField, T]) {
    def toValidationResult: Result[F, T] = EitherT.fromEither(
      validated
        .leftMap(errors =>
          ValidationFailed(
            message = "User validation failed.",
            errors.toNonEmptyList.toList
          )
        )
        .toEither
    )
  }
}
