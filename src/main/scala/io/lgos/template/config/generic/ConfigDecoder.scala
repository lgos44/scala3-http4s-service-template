package io.lgos.template.config.generic

import com.typesafe.config.{Config, ConfigFactory}

import cats.implicits.*
import cats.data.*
import scala.collection.IterableFactory.*
import scala.collection.JavaConverters.*
import scala.collection.{Factory, IterableFactory}
import scala.deriving.Mirror
import scala.util.{Failure, Success, Try}

trait ConfigDecoder[T] {
  def decode(
    str: Config,
    path: String
  ): Either[ConfigError, T]
}

object ConfigDecoder {
  import scala.compiletime.*
  import scala.deriving.*

  type DecodeResultAccumulating[T] = Either[NonEmptyChain[ConfigError], T]

  type DecodeResult[T] = Either[ConfigError, T]

  inline private def baseDecoder[T](f: (Config, String) => T) = new ConfigDecoder[T] {
    override def decode(
      config: Config,
      path: String
    ): DecodeResult[T] =
      Either
        .catchNonFatal {
          f(config, path)
        }
        .left
        .map { (error: Throwable) =>
          ParsingError(config.toString, path, error)
        }
  }

  inline given boolDec: ConfigDecoder[Boolean] = baseDecoder { case (config, path) => config.getBoolean(path) }

  inline given stringDec: ConfigDecoder[String] = baseDecoder { case (config, path) => config.getString(path) }

  inline given intDec: ConfigDecoder[Int] = baseDecoder { case (config, path) => config.getInt(path) }

  inline given longDec: ConfigDecoder[Long] = baseDecoder { case (config, path) => config.getLong(path) }

  inline given doubleDec: ConfigDecoder[Double] = baseDecoder { case (config, path) => config.getDouble(path) }

  inline given listDecoder[T](using d: ConfigDecoder[T]): ConfigDecoder[List[T]] = new ConfigDecoder[List[T]] {
    override def decode(
      config: Config,
      path: String
    ): DecodeResult[List[T]] =
      ParsingError.catchNonFatal(config, path)(config.getList(path).unwrapped().asScala.toList.map(_.asInstanceOf[T]))
  }

  inline given seqDecoder[T](using d: ConfigDecoder[T]): ConfigDecoder[Seq[T]] = new ConfigDecoder[Seq[T]] {
    override def decode(
      config: Config,
      path: String
    ): DecodeResult[Seq[T]] =
      ParsingError.catchNonFatal(config, path)(config.getList(path).unwrapped().asScala.toSeq.map(_.asInstanceOf[T]))
  }

  inline given setDecoder[T](using d: ConfigDecoder[T]): ConfigDecoder[Set[T]] = new ConfigDecoder[Set[T]] {
    override def decode(
      config: Config,
      path: String
    ): DecodeResult[Set[T]] =
      ParsingError.catchNonFatal(config, path)(config.getList(path).unwrapped().asScala.toSet.map(_.asInstanceOf[T]))
  }

  inline def derived[T](using m: Mirror.Of[T]): ConfigDecoder[T] = {
    val elemInstances = summonAll[m.MirroredElemTypes]
    val labels = elemLabels[m.MirroredElemLabels]
    inline m match {
      case p: Mirror.ProductOf[T] => productDecoder(p, elemInstances, labels)
      case s: Mirror.SumOf[T] => error("Sum types not supported")
    }
  }

  inline def summonAll[T <: Tuple]: List[ConfigDecoder[_]] = inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts) => summonInline[ConfigDecoder[t]] :: summonAll[ts]
  }

  inline def elemLabels[T <: Tuple]: List[String] = inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts) => constValue[t].asInstanceOf[String] :: elemLabels[ts]
  }

  def productDecoder[T](
    p: Mirror.ProductOf[T],
    elems: List[ConfigDecoder[_]],
    labels: List[String]
  ): ConfigDecoder[T] =
    new ConfigDecoder[T] {
      def decode(
        config: Config,
        path: String
      ): DecodeResult[T] = {
        val thisConfig = if (path.isEmpty) config else config.getConfig(path)
        val decodedList = labels
          .zip(elems)
          .map { case (label, decoder) =>
            decoder.decode(thisConfig, label)
          }
        val decoded = decodedList.partitionMap(identity) match {
          case (Nil, rights) => Right(rights)
          case (lefts, _) => Left(lefts)
        }

        decoded match {
          case Right(d) =>
            val res = d.foldRight[Tuple](EmptyTuple)(_ *: _)
            Right(p.fromProduct(res))
          case Left(list) =>
            Left(list.head)
        }
      }
    }
}
