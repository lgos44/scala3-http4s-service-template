package io.lgos.template

import cats.data.NonEmptyList
import cats.effect.IO
import io.lgos.template.util.ServerEndpoints
import io.lgos.template.api.HttpServer
import io.lgos.template.config.{AppConfig, ConfigModule}
import io.lgos.template.metrics.MetricsModule
import io.lgos.template.user.UserModule

trait AppModule extends UserModule with MetricsModule with ConfigModule {

  // Concatenates endpoints from all modules
  lazy val endpoints: ServerEndpoints[IO] = userRoutes.endpoints

  lazy val adminEndpoints: ServerEndpoints[IO] =
    NonEmptyList.of(versionApi.versionEndpoint)

  // Unfortutately on scala3 we cant wire from super class
  // https://github.com/lampepfl/dotty/issues/13105
  lazy val httpServer: HttpServer = new HttpServer(
    endpoints,
    adminEndpoints,
    prometheusMetrics,
    config.server
  )
}
