package io.lgos.template.config

import io.lgos.template.config.generic.ConfigDecoder

case class ServerConfig(
  host: String,
  port: Int)
  derives ConfigDecoder
