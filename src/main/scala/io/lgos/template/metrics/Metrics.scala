package io.lgos.template.metrics

import io.prometheus.client.{hotspot, Counter, Gauge}

object Metrics {
  lazy val registeredUsersCounter: Counter =
    Counter
      .build()
      .name(s"registered_users_counter")
      .help(s"How many users registered")
      .register()

  def init(): Unit =
    hotspot.DefaultExports.initialize()
}
