package io.lgos.template.test

import cats.effect.IO
import io.lgos.template.{AppModule, ResourceModule}
import io.prometheus.client.CollectorRegistry
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

/**
  * Contains all app components that might be needed to setup a test version and wire it injecting any test specific
  * dependencies. Right now it just wires it as usual.
  * TODO: Allow injecting test dependencies (mocks etc)
  */
trait TestModule extends AppModule {
  override lazy val prometheusMetrics: PrometheusMetrics[IO] =
    PrometheusMetrics.default[IO](registry = new CollectorRegistry())
}

trait TestResources extends ResourceModule
