package io.lgos.template.metrics

import cats.effect.IO
import com.softwaremill.macwire.wireWith
import io.prometheus.client.CollectorRegistry
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

trait MetricsModule {
  lazy val versionApi: VersionRoutes[IO] = wireWith(VersionRoutes.apply _)
  lazy val prometheusMetrics: PrometheusMetrics[IO] =
    PrometheusMetrics.default[IO](registry = CollectorRegistry.defaultRegistry)
}
