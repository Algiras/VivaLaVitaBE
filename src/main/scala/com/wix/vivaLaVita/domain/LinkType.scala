package com.wix.vivaLaVita.domain

import enumeratum._

sealed trait LinkType extends EnumEntry

case object LinkType extends Enum[LinkType] with CirceEnum[LinkType] {
  case object LinkedIn extends LinkType
  case object Facebook extends LinkType
  case object Github extends LinkType

  val values = findValues
}
