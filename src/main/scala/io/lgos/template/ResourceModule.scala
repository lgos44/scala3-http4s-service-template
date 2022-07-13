package io.lgos.template

import cats.effect.{IO, Resource}
import io.lgos.template.config.ConfigModule
import io.lgos.template.infrastructure.{DB, Doobie}

/**
  * Module to bundle up all needed resources
  */
trait ResourceModule extends ConfigModule {
  lazy val xa: Resource[IO, Doobie.Transactor[IO]] = DB.initializeDb(config.db)

  lazy val resources: Resource[IO, Resources] = xa.map(Resources.apply)
}

case class Resources(xa: Doobie.Transactor[IO])
