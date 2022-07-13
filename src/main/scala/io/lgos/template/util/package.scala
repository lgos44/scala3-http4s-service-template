package io.lgos.template

import cats.data.NonEmptyList
import sttp.tapir.server.ServerEndpoint

package object util {

  type ServerEndpoints[F[_]] = NonEmptyList[ServerEndpoint[Any, F]]

}
