package io.lgos.template.infrastructure

import com.typesafe.scalalogging.StrictLogging
import doobie.util.log.{ExecFailure, ProcessingFailure, Success}

import scala.concurrent.duration.*

object Doobie
  extends doobie.Aliases
  with doobie.hi.Modules
  with doobie.free.Modules
  with doobie.free.Types
  with doobie.postgres.Instances
  with doobie.util.meta.LegacyInstantMetaInstance
  with doobie.free.Instances
  with doobie.syntax.AllSyntax
  with StrictLogging {

  private val SlowThreshold = 200.millis

  // Tracks slow or failed queries
  given doobieLogHandler: LogHandler = LogHandler {
    case Success(sql, _, exec, processing) =>
      if (exec > SlowThreshold || processing > SlowThreshold) {
        logger.warn(s"Slow query (execution: $exec, processing: $processing): $sql")
      }
    case ProcessingFailure(sql, args, exec, processing, failure) =>
      logger.error(s"Processing failure (execution: $exec, processing: $processing): $sql | args: $args", failure)
    case ExecFailure(sql, args, exec, failure) =>
      logger.error(s"Execution failure (execution: $exec): $sql | args: $args", failure)
  }
}
