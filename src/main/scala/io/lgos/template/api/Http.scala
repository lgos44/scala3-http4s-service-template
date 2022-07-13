package io.lgos.template.api

import cats.effect.Async
import cats.implicits.*
import io.lgos.template.common.Failure.*
import io.lgos.template.infrastructure.Json.*
import io.circe.{Encoder, Json, Printer}
import sttp.tapir.generic.auto.SchemaDerivation
import sttp.tapir.json.circe.TapirJsonCirce
import sttp.tapir.*
import sttp.model.StatusCode
import com.typesafe.scalalogging.StrictLogging
import io.lgos.template.logging.FLogging.*
import io.lgos.template.common.{Failure, Result}
import io.lgos.template.common.Failure.{Conflict, NotFound, Unauthorized, ValidationFailed}

object Http extends Tapir with TapirJsonCirce with SchemaDerivation with StrictLogging {

  val defaultUnauthorized: EndpointOutput.OneOfVariant[Unauthorized] = oneOfVariant(
    statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized].description("Unauthorized"))
  )

  val defaultValidationFailed: EndpointOutput.OneOfVariant[ValidationFailed] = oneOfVariant(
    statusCode(StatusCode.BadRequest).and(jsonBody[ValidationFailed].description("Validation failed"))
  )

  val defaultConflict: EndpointOutput.OneOfVariant[Conflict] = oneOfVariant(
    statusCode(StatusCode.Conflict).and(jsonBody[Conflict].description("Conflict"))
  )

  val defaultNotFound: EndpointOutput.OneOfVariant[NotFound] = oneOfVariant(
    statusCode(StatusCode.NotFound).and(jsonBody[NotFound].description("Not found"))
  )

  val jsonErrorOutOutput: EndpointOutput[ErrorResponse] = jsonBody[ErrorResponse]

  implicit class FOut[F[_]: Async, T](f: F[T]) {
    def toOut: F[Either[Failure, T]] =
      f.map(t => t.asRight[Failure]).recoverWith { case f: Failure =>
        logger.warnF(s"Request failed: ${f.message}") >>
          Async[F].delay(f).map(_.asLeft[T])
      }
  }

  override def jsonPrinter: Printer = noNullsPrinter
}

case class ErrorResponse(message: String)
