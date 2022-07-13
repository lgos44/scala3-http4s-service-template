package io.lgos.template.test

import PgContainer.container
import com.typesafe.scalalogging.StrictLogging
import io.lgos.template.config.DbConfig
import org.flywaydb.core.Flyway
import org.postgresql.jdbc.PgConnection
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.testcontainers.containers.{JdbcDatabaseContainer, PostgreSQLContainerProvider}
import org.testcontainers.jdbc.ConnectionUrl

/**
  * Starts postgres container and prepares/cleans db for each test.
  */
trait PgContainer extends BeforeAndAfterEach with BeforeAndAfterAll with StrictLogging { self: Suite =>

  lazy val dbConfig: DbConfig = DbConfig(
    user = container.getUsername,
    password = container.getPassword,
    url = container.getJdbcUrl,
    migrateOnStart = true,
    driver = "org.postgresql.ds.PGSimpleDataSource",
    connectThreadPoolSize = 10
  )

  private lazy val flyway: Flyway = Flyway
    .configure()
    .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
    .load()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    flyway.migrate()
  }

  override protected def afterEach(): Unit = {
    flyway.clean()
    super.afterEach()
  }
}

object PgContainer extends StrictLogging {
  lazy val container: JdbcDatabaseContainer[_] = {
    val url =
      ConnectionUrl.newInstance("jdbc:tc:postgresql:13-alpine:///test")
    start(new PostgreSQLContainerProvider().newInstance(url))
  }

  /**
    * Avoids restarting container for every test class but makes sure its stopped after all
    * @param container postgres container
    * @return
    */
  private[this] def start(
    container: JdbcDatabaseContainer[_]
  ): JdbcDatabaseContainer[_] = {
    logger.info("Starting Postgres container...")
    container.start()
    logger.info("Postgres container started")

    Runtime.getRuntime
      .addShutdownHook(new Thread {
        override def start(): Unit = {
          logger.info("Stopping Postgres container...")
          container.stop()
          logger.info("Postgres container stopped")
        }
      })
    container
  }

}
