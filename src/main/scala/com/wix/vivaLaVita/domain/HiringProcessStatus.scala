package com.wix.vivaLaVita.domain

import enumeratum._

sealed trait HiringProcessStatus extends EnumEntry

case object HiringProcessStatus extends Enum[HiringProcessStatus] with CirceEnum[HiringProcessStatus] {
  case object Active extends HiringProcessStatus
  case object Hired extends HiringProcessStatus
  case object Rejected extends HiringProcessStatus

  val values = findValues
}


