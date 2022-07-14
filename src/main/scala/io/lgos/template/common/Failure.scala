package io.lgos.template.common

import io.circe.{Decoder, Encoder, Json}
import io.circe.HCursor
import sttp.tapir.generic.auto.*

sealed trait Failure extends Exception {
  def message: String
}

object Failure {

  case class NotFound(message: String) extends Failure
  case class Conflict(message: String) extends Failure
  case class BadRequest(message: String) extends Failure
  case class Unauthorized(message: String) extends Failure
  case object Forbidden extends Failure {
    def message: String = "Forbidden"
  }

  case class UnexpectedError(
    message: String)
    extends Failure

  case class ValidationFailed(
    message: String,
    errors: List[InvalidField])
    extends Failure

  case class InvalidField(
    name: String,
    value: String,
    reason: String)

}
