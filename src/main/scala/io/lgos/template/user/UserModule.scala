package io.lgos.template.user

import cats.effect.IO
import cats.implicits.*
import doobie.util.transactor.Transactor
import com.softwaremill.macwire.*

class testWiring(first: Double)

trait UserModule {
  def xa: Transactor[IO]
  lazy val userRepo: UserRepository[IO] = wireWith(UserRepository.implIO _)
  lazy val userService: UserService[IO] = wireWith(UserService.implIO _)
  lazy val userRoutes: UserRoutes[IO] = wireWith(UserRoutes.implIO _)
}
