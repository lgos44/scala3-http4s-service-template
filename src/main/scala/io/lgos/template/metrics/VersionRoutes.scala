package io.lgos.template.metrics

import cats.effect.{Async, IO}
import io.lgos.template.api.Http
import sttp.tapir.server.ServerEndpoint
import io.lgos.template.infrastructure.Json.*
import io.lgos.template.common.Failure
import io.lgos.template.version.BuildInfo
import sttp.model.StatusCode

class VersionRoutes[F[_]: Async] {
  import io.lgos.template.api.Http.*
  import VersionRoutes.*

  val versionEndpoint: ServerEndpoint[Any, F] = endpoint.get
    .in("version")
    .out(jsonBody[VersionResponse])
    .errorOut(statusCode(StatusCode.InternalServerError).and(jsonBody[Failure]))
    .serverLogic { _ =>
      Async[F].pure(VersionResponse()).toOut
    }
}

object VersionRoutes {
  def apply(): VersionRoutes[IO] = new VersionRoutes[IO]
  case class VersionResponse(
      name: String,
      version: String,
      scalaVersion: String,
      sbtVersion: String,
      buildSha: String)
  object VersionResponse {
    def apply(): VersionResponse = VersionResponse(
      name = BuildInfo.name,
      version = BuildInfo.version,
      scalaVersion = BuildInfo.scalaVersion,
      sbtVersion = BuildInfo.sbtVersion,
      buildSha = BuildInfo.lastCommitHash
    )
  }
}
