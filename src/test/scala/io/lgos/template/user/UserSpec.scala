package io.lgos.template.user

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import io.lgos.template.common.Failure.*
import io.lgos.template.test.Fixtures.{withStdFixture, Fixture}
import org.scalatest.concurrent.Eventually
import UserRoutes.*
import UserSpec.*
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import sttp.client3
import sttp.client3.{basicRequest, Response, SttpBackend, UriContext}
import sttp.model.StatusCode
import io.circe.generic.auto.*
import sttp.client3.*
import sttp.client3.circe.*
import io.lgos.template.test.TestUtils.*
import io.lgos.template.common.Failure
import io.lgos.template.test.{BaseSpec, DefaultTestConfig, Fixtures, PgContainer}
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils

import scala.util.Random

class UserSpec extends BaseSpec with PgContainer with DefaultTestConfig with AsyncIOSpec {
  "registerUserEndpoint" should {
    "successfully register new user" in withStdFixture() { implicit f =>
      val (login, email, password) = randomLoginEmailPassword()
      for {
        registerResponse <- registerUser(login, email, password)
        body = registerResponse.body.shouldDeserializeTo[AuthenticatedUserResponse]
      } yield {
        registerResponse.code shouldBe StatusCode.Created
        body.username shouldBe login
        body.email shouldBe email
      }
    }

    "return Conflict for repeated username or email" in withStdFixture() { implicit f =>
      val (login, email, password) = randomLoginEmailPassword()
      for {
        _ <- registerUser(login, email, password)
        registerDuplicate <- registerUser(login, email, password)
        _ = registerDuplicate.body.shouldDeserializeToError[Conflict]
      } yield registerDuplicate.code shouldBe StatusCode.Conflict
    }

    "return BadRequest for invalid body" in withStdFixture() { implicit f =>
      for {
        response <- registerUser("{}")
        _ = response.body.shouldDeserializeToError[BadRequest]
      } yield response.code shouldBe StatusCode.BadRequest
    }

    "return BadRequest for validation error" in withStdFixture() { implicit f =>
      val (login, email, password) = ("_validation", "should", "fail")
      for {
        response <- registerUser(login, email, password)
        details = response.body.shouldDeserializeToError[ValidationFailed]
      } yield {
        response.code shouldBe StatusCode.BadRequest
        details.errors should contain(InvalidField("username", login, "Username does not match requirements."))
        details.errors should contain(InvalidField("email", email, "Is not a valid email."))
        details.errors should contain(InvalidField("password", password, "Password too short"))
      }
    }
  }

  "getUserEndpoint" should {
    "return existing user" in withStdFixture() { implicit f =>
      val (username, email, password) = randomLoginEmailPassword()
      for {
        registerResponse <- registerUser(username, email, password)
        body = registerResponse.body.shouldDeserializeTo[AuthenticatedUserResponse]
        userResponse <- getUser(body.id)
        user = userResponse.body.shouldDeserializeTo[UserResponse]
      } yield {
        userResponse.code shouldBe StatusCode.Ok
        user shouldBe UserResponse(body.id, email, username, None, None)
      }
    }

    "return not found for used that does not exist" in withStdFixture() { implicit f =>
      for {
        userResponse <- getUser(1L)
        _ = userResponse.body.shouldDeserializeToError[NotFound]
      } yield userResponse.code shouldBe StatusCode.NotFound
    }
  }

  "updateUserEndpoint" should {
    "update user" in withStdFixture() { implicit f =>
      val (username, email, password) = randomLoginEmailPassword()
      val newUsername = "newUsername"
      val newEmail = "newEmail@email.com"
      val newName = Some("newName")
      val newLastName = Some("newLastName")
      for {
        registerResponse <- registerUser(username, email, password)
        body = registerResponse.body.shouldDeserializeTo[AuthenticatedUserResponse]
        updateResponse <- updateUser(body.id, newEmail, newUsername, newName, newLastName)
        _ = updateResponse.body.shouldDeserializeTo[UserResponse]
        getUserResponse <- getUser(body.id)
        user = getUserResponse.body.shouldDeserializeTo[UserResponse]
      } yield {
        updateResponse.code shouldBe StatusCode.Ok
        user.username shouldBe newUsername
        user.email shouldBe newEmail
        user.firstName shouldBe newName
        user.lastName shouldBe newLastName
      }
    }

    "return not found for used that does not exist" in withStdFixture() { implicit f =>
      val newUsername = "newUsername"
      val newEmail = "newEmail@email.com"
      val newName = Some("newName")
      val newLastName = Some("newLastName")
      for {
        userResponse <- updateUser(1L, newEmail, newUsername, newName, newLastName)
        _ = userResponse.body.shouldDeserializeToError[NotFound]
      } yield userResponse.code shouldBe StatusCode.NotFound
    }
  }
}

object UserSpec {
  private val basePath = "http://localhost:8080/api/v1"
  private val random = new Random()

  def randomLoginEmailPassword(): (String, String, String) =
    (
      RandomStringUtils.randomAlphanumeric(12),
      s"user${random.nextInt(9000)}@gmail.com",
      RandomStringUtils.randomAlphanumeric(12)
    )

  def registerUser(
    login: String,
    email: String,
    password: String
  )(using fixture: Fixture
  ): IO[Response[Either[String, String]]] = registerUser(RegisterRequest(login, email, password).asJson.noSpaces)

  def registerUser(
    body: String
  )(using fixture: Fixture
  ): IO[Response[Either[String, String]]] =
    basicRequest
      .post(uri"$basePath/users")
      .body(body)
      .send(fixture.sttpBackend)

  def getUser(
    id: Long
  )(using fixture: Fixture
  ): IO[Response[Either[String, String]]] =
    basicRequest
      .get(uri"$basePath/users/${id}")
      .send(fixture.sttpBackend)

  def updateUser(
    id: Long,
    email: String,
    username: String,
    firstName: Option[String],
    lastName: Option[String]
  )(using fixture: Fixture
  ): IO[Response[Either[String, String]]] =
    basicRequest
      .put(uri"$basePath/users/${id}")
      .body(UpdateUserRequest(email, username, firstName, lastName).asJson.noSpaces)
      .send(fixture.sttpBackend)
}
