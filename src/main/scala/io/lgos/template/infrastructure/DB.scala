package io.lgos.template.infrastructure

import cats.effect.{Async, IO, Resource}
import Doobie.*
import com.typesafe.scalalogging.StrictLogging
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import io.lgos.template.config.DbConfig

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

object DB extends StrictLogging {

  def dbTransactor[F[_]: Async](
    config: DbConfig,
    connEc: ExecutionContext
  ): Resource[F, HikariTransactor[F]] =
    HikariTransactor
      .newHikariTransactor[F](config.driver, config.url, config.user, config.password, connEc)

  def initializeDb(config: DbConfig): Resource[IO, Transactor[IO]] = {
    val flyway: Flyway =
      Flyway
        .configure()
        .dataSource(config.url, config.user, config.password)
        .load()

    val res = for {
      ec <- doobie.util.ExecutionContexts
        .fixedThreadPool[IO](config.connectThreadPoolSize)
      transactor <- dbTransactor[IO](config, ec)
    } yield transactor
    res.evalTap(xa => connectAndMigrate(config, flyway, xa))
  }

  private def connectAndMigrate(
    config: DbConfig,
    flyway: Flyway,
    xa: Transactor[IO]
  ): IO[Unit] =
    (migrate(config, flyway) >> testConnection(xa) >> IO(logger.info("Database migration & connection test complete")))
      .onError { (e: Throwable) =>
        logger.warn("Database not available, waiting 5 seconds to retry...", e)
        IO.sleep(5.seconds) >> connectAndMigrate(config, flyway, xa)
      }

  private def migrate(
    config: DbConfig,
    flyway: Flyway
  ): IO[Unit] =
    if (config.migrateOnStart) {
      IO(flyway.migrate()).void
    } else IO.unit

  private def testConnection(xa: Transactor[IO]): IO[Unit] =
    IO {
      sql"select 1".query[Int].unique.transact(xa)
    }.void
}
