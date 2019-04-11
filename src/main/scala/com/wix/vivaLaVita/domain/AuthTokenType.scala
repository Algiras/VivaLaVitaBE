package com.wix.vivaLaVita.domain

import enumeratum._

sealed trait AuthTokenType extends EnumEntry

case object AuthTokenType extends Enum[AuthTokenType] with CirceEnum[AuthTokenType] {
  case object Google extends AuthTokenType

  val values = findValues
}


