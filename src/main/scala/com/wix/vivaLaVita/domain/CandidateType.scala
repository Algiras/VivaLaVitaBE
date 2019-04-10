package com.wix.vivaLaVita.domain

import enumeratum._

sealed trait CandidateType extends EnumEntry

case object CandidateType extends Enum[CandidateType] with CirceEnum[CandidateType] {
  case object Lead extends CandidateType
  case object Referral extends CandidateType

  val values = findValues
}
