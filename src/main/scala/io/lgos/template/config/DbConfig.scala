package io.lgos.template.config

import cats.syntax.functor.*
import cats.effect.{Async, Resource, Sync}

import scala.concurrent.ExecutionContext
import doobie.hikari.HikariTransactor
import io.lgos.template.config.generic.ConfigDecoder
import org.flywaydb.core.Flyway

case class DbConnectionsConfig(
  poolSize: Int)
  derives ConfigDecoder

case class DbConfig(
  user: String,
  password: String,
  url: String,
  migrateOnStart: Boolean,
  driver: String,
  connectThreadPoolSize: Int)
  derives ConfigDecoder
