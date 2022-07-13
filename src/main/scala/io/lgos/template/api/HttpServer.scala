package io.lgos.template.api

import cats.effect.{Async, IO, Resource}
import com.comcast.ip4s.{Host, Port}
import com.typesafe.scalalogging.StrictLogging
import org.http4s.ember.server.EmberServerBuilder
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

import io.lgos.template.util.*
import io.lgos.template.config.ServerConfig
import io.lgos.template.logging.FLogging.*

/**
  * Builds Http4s server from Tapir endpoint descriptions.
  *
  * Endpoint structure as follows:
  *   - `/api/v1` - main API
  *   - `/api/v1/docs` - swagger UI for the main API
  *   - `/admin` - admin API
  */
class HttpServer(
  mainEndpoints: ServerEndpoints[IO],
  adminEndpoints: ServerEndpoints[IO],
  prometheusMetrics: PrometheusMetrics[IO],
  config: ServerConfig)
  extends StrictLogging {
  private val apiContextPath = List("api", "v1")

  val serverOptions: Http4sServerOptions[IO] = Http4sServerOptions
    .customiseInterceptors[IO]
    // all errors are formatted as json, and there are no other additional http4s routes
    .defaultHandlers(
      msg => ValuedEndpointOutput(Http.jsonErrorOutOutput, ErrorResponse(msg)),
      notFoundWhenRejected = true
    )
    .serverLog {
      Http4sServerOptions
        .defaultServerLog[IO]
    }
    .corsInterceptor(CORSInterceptor.default[IO])
    .metricsInterceptor(prometheusMetrics.metricsInterceptor())
    .options

  lazy val allEndpoints: List[ServerEndpoint[Any, IO]] = {
    // only document main endpoints
    val docsEndpoints =
      SwaggerInterpreter(swaggerUIOptions = SwaggerUIOptions.default.copy(contextPath = apiContextPath))
        .fromServerEndpoints(mainEndpoints.toList, "Template", "0.1")

    val apiEndpoints =
      (mainEndpoints ++ docsEndpoints).map(se =>
        se.prependSecurityIn(apiContextPath.foldLeft(emptyInput: EndpointInput[Unit])(_ / _))
      )

    val allAdminEndpoints =
      (adminEndpoints ++ List(prometheusMetrics.metricsEndpoint)).map(_.prependSecurityIn("admin"))

    apiEndpoints.toList ++ allAdminEndpoints.toList
  }

  lazy val routes: HttpRoutes[IO] = Http4sServerInterpreter[IO](serverOptions).toRoutes(allEndpoints)

  /** The resource describing the HTTP server; binds when the resource is allocated. */
  lazy val resource: Resource[IO, org.http4s.server.Server] = EmberServerBuilder
    .default[IO]
    .withHost(Host.fromString(config.host).get)
    .withPort(Port.fromInt(config.port).get)
    .withHttpApp(routes.orNotFound)
    .build

}
