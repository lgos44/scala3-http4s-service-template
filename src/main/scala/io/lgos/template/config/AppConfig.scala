package io.lgos.template.config

import io.lgos.template.config.generic.ConfigDecoder

case class AppConfig(
  db: DbConfig,
  server: ServerConfig)
  derives ConfigDecoder
