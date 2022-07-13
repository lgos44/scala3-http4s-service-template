package io.lgos.template.infrastructure

import io.circe.generic.AutoDerivation
import io.circe.{Decoder, Encoder, Printer}

object Json extends AutoDerivation {
  val noNullsPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
}
