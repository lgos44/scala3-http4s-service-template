package io.lgos.template

import cats.effect.{IO, Resource, ResourceApp}
import cats.effect.implicits.*
import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import doobie.util.transactor
import io.lgos.template.config.AppConfig
import io.lgos.template.config.generic.parser
import io.lgos.template.metrics.Metrics

import concurrent.ExecutionContext.Implicits.global

object Main extends ResourceApp.Forever with StrictLogging {
  Metrics.init()

  def run(args: List[String]): Resource[IO, Unit] = {
    val resourceModule = new ResourceModule {}
    resourceModule.resources
      .flatMap { res =>
        val modules = new AppModule:
          override def xa: transactor.Transactor[IO] = res.xa
        modules.httpServer.resource
      }
      .as(())
  }
}
