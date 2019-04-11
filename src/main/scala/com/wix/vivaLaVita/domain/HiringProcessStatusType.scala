package com.wix.vivaLaVita.domain

import enumeratum._

sealed trait HiringProcessStatusType extends EnumEntry

case object HiringProcessStatusType extends Enum[HiringProcessStatusType] with CirceEnum[HiringProcessStatusType] {
  case object Active extends HiringProcessStatusType
  case object Hired extends HiringProcessStatusType
  case object Rejected extends HiringProcessStatusType

  val values = findValues
}


