package com.wix.vivaLaVita.dto

import java.util.UUID

import cats.effect.Sync
import com.wix.vivaLaVita.domain.{HiringProcessId, _}
import com.wix.vivaLaVita.dto.CandidateDTO._
import com.wix.vivaLaVita.dto.PositionDTO._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime
import shapeless.tag

object HiringProcessDTO {
  implicit val HiringProcessStatusEncoder: Encoder[HiringProcessStatus] = deriveEncoder
  implicit val HiringProcessStatusDecoder: Decoder[HiringProcessStatus] = deriveDecoder

  case class HiringProcessResponse(id: HiringProcessId, position: PositionResponse, candidate: CandidateResponse, status: HiringProcessStatus, createdAt: DateTime, updatedAt: Option[DateTime])
  implicit val HiringProcessResponseEncoder: Encoder[HiringProcessResponse] = deriveEncoder

  def buildResponse(hiringProcess: HiringProcess, position: Position, candidate: Candidate): HiringProcessResponse = HiringProcessResponse(
    id = hiringProcess.id,
    position = PositionDTO.buildResponse(position),
    candidate = CandidateDTO.buildResponse(candidate),
    status = hiringProcess.status,
    createdAt = hiringProcess.createdAt,
    updatedAt = hiringProcess.updatedAt
  )

  case class HiringProcessRequest(positionId: PositionId, candidateId: CandidateId, status: HiringProcessStatus)
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

  case class HiringProcessUpdateRequest(status: HiringProcessStatus)
  implicit val HiringProcessUpdateRequestDecoder: Decoder[HiringProcessUpdateRequest] = deriveDecoder
  implicit def HiringProcessUpdatedRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, HiringProcessUpdateRequest] =
    jsonOf[F, HiringProcessUpdateRequest]

  def buildUpdatedHiringProcess(hiringProcess: HiringProcess, hiringProcessUpdate: HiringProcessRequest): HiringProcess = {
    hiringProcess.copy(
      status = hiringProcessUpdate.status,
      updatedAt = Some(DateTime.now)
    )
  }
}
