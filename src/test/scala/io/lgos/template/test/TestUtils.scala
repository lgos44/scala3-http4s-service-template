package io.lgos.template.test

import cats.effect.IO
import io.circe.{parser, Decoder}
import io.circe.generic.auto.*
import scala.reflect.ClassTag

object TestUtils {

  extension (r: Either[String, String]) {
    def shouldDeserializeTo[T: Decoder: ClassTag]: T =
      r.flatMap(parser.parse).flatMap(_.as[T]).right.get

    def shouldDeserializeToError[T: Decoder: ClassTag]: T =
      parser.parse(r.left.get).flatMap(_.as[T]).right.get
  }

  extension (r: IO[Either[String, String]]) {
    def shouldDeserializeTo[T: Decoder: ClassTag]: IO[T] =
      r.map(_.flatMap(parser.parse).flatMap(_.as[T]).right.get)

    def shouldDeserializeToError[T: Decoder: ClassTag]: IO[T] =
      r.map(e => parser.parse(e.left.get).flatMap(_.as[T]).right.get)
  }

}
