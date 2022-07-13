package io.lgos.template.config

import io.lgos.template.config.generic.parser

trait ConfigModule {
  lazy val config: AppConfig = parser.decodeUnsafe[AppConfig]
}
