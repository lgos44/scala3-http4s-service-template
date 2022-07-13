package io.lgos.template.metrics

import cats.effect.{Async, IO}
import io.lgos.template.api.Http
import sttp.tapir.server.ServerEndpoint
import io.lgos.template.infrastructure.Json.*
import io.lgos.template.common.Failure
import sttp.model.StatusCode

class VersionRoutes[F[_]: Async] {
  import io.lgos.template.api.Http.*
  import VersionRoutes.*

  val versionEndpoint: ServerEndpoint[Any, F] = endpoint.get
    .in("version")
    .out(jsonBody[VersionResponse])
    .errorOut(statusCode(StatusCode.InternalServerError).and(jsonBody[Failure]))
    .serverLogic { _ =>
      Async[F].pure(VersionResponse("0.1")).toOut
    }
}

object VersionRoutes {
  def apply(): VersionRoutes[IO] = new VersionRoutes[IO]
  case class VersionResponse(buildSha: String)
}
