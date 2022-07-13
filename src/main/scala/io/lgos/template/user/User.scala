package io.lgos.template.user

import UserRoutes.{AuthenticatedUserResponse, UserResponse}

final case class User(
  id: Long,
  username: String,
  email: String,
  password: String,
  firstName: Option[String] = None,
  lastName: Option[String] = None) {

  def toAuthenticatedResponse: AuthenticatedUserResponse =
    AuthenticatedUserResponse(
      id,
      email,
      username,
      "token",
      firstName,
      lastName
    )

  def toResponse: UserResponse =
    UserResponse(
      id,
      email,
      username,
      firstName,
      lastName
    )
}

object User {
  def empty: User = User(0, "", "", "", None, None)

}
