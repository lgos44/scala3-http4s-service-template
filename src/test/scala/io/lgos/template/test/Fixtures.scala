package io.lgos.template.test

import cats.effect.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.http4s.*
import doobie.util.transactor
import io.lgos.template.{AppModule, ResourceModule}
import io.lgos.template.config.AppConfig
import org.http4s.ember.client.EmberClientBuilder

object Fixtures {

  /**
    * Should contain all components need to run a test.
    * @param modules Test version of the app
    * @param sttpBackend http backend used to run requests against the app
    */
  case class Fixture(
    modules: TestModule,
    sttpBackend: SttpBackend[IO, Fs2Streams[IO]])

  /**
    * This builds a fixture object and provides it to a test.
    * @param test
    * @param conf
    * @return
    */
  def withStdFixture()(test: Fixture => IO[Unit])(using conf: AppConfig): IO[Unit] = {
    val resourceModule = new TestResources {
      override lazy val config: AppConfig = conf
    }
    val fixture = for {
      resources <- resourceModule.resources
      modules = new TestModule:
        override lazy val config: AppConfig = conf
        override def xa: transactor.Transactor[IO] = resources.xa
      _ <- modules.httpServer.resource
      client <- EmberClientBuilder.default[IO].build
      backend = Http4sBackend.usingClient[IO](client)
    } yield Fixture(modules, backend)
    fixture.use(test)
  }
}
