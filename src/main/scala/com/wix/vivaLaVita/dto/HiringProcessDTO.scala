package com.wix.vivaLaVita.dto

import java.util.UUID

import cats.effect.Sync
import com.wix.vivaLaVita.domain.{HiringProcessId, _}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime
import shapeless.tag
import io.circe.syntax._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._

object HiringProcessDTO {

  implicit val HiringProcessStatusEncoder: Encoder[HiringProcessStatusType] = {
    // Fixing random bug with Circe by writing encoder by hand
    hiringProcessStatusType: HiringProcessStatusType => Json.fromString(hiringProcessStatusType.entryName)
  }
  implicit val HiringProcessStatusDecoder: Decoder[HiringProcessStatusType] = deriveDecoder

  case class HiringProcessResponse(id: HiringProcessId, positionId: PositionId, candidateId: CandidateId, status: HiringProcessStatusType, createdAt: DateTime, updatedAt: Option[DateTime])
  implicit val HiringProcessResponseEncoder: Encoder[HiringProcessResponse] = deriveEncoder

  def buildResponse(hiringProcess: HiringProcess): HiringProcessResponse = HiringProcessResponse(
    id = hiringProcess.id,
    positionId = hiringProcess.positionId,
    candidateId = hiringProcess.candidateId,
    status = hiringProcess.status,
    createdAt = hiringProcess.createdAt,
    updatedAt = hiringProcess.updatedAt
  )

  case class HiringProcessRequest(positionId: PositionId, candidateId: CandidateId, status: HiringProcessStatusType)
  implicit val HiringProcessRequestDecoder: Decoder[HiringProcessRequest] = deriveDecoder
  implicit def HiringProcessRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, HiringProcessRequest] =
    jsonOf[F, HiringProcessRequest]

  def buildHiringProcess(hiringProcess: HiringProcessRequest): HiringProcess = HiringProcess(
    id = tagUUIDAsHiringProcessId(UUID.randomUUID()),
    positionId = hiringProcess.positionId,
    candidateId = hiringProcess.candidateId,
    status =  hiringProcess.status,
    createdAt = DateTime.now(),
    updatedAt = None,
    isActive = true
  )

  case class HiringProcessUpdateRequest(status: HiringProcessStatusType)
  implicit val HiringProcessUpdateRequestDecoder: Decoder[HiringProcessUpdateRequest] = deriveDecoder
  implicit def HiringProcessUpdatedRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, HiringProcessUpdateRequest] =
    jsonOf[F, HiringProcessUpdateRequest]

  def buildUpdatedHiringProcess(hiringProcess: HiringProcess, hiringProcessUpdate: HiringProcessUpdateRequest): HiringProcess = {
    hiringProcess.copy(
      status = hiringProcessUpdate.status,
      updatedAt = Some(DateTime.now)
    )
  }
}
