package io.lgos.template.test

import io.lgos.template.config.{AppConfig, DbConfig}
import io.lgos.template.config.generic.parser

trait DefaultTestConfig {
  def dbConfig: DbConfig
  given config: AppConfig = parser.decodeUnsafe[AppConfig].copy(db = dbConfig)
}
