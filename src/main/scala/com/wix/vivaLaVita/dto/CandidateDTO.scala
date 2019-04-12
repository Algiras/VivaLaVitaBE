package com.wix.vivaLaVita.dto

import java.util.UUID

import cats.effect.Sync
import com.wix.vivaLaVita.domain.{CandidateId, _}
import com.wix.vivaLaVita.dto.MessageDTO.MessageRequest
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime
import shapeless.tag

object CandidateDTO {
  implicit val LinkEncoder: Encoder[Link] = deriveEncoder
  implicit val LinkDecoder: Decoder[Link] = deriveDecoder

  case class CandidateResponse(id: CandidateId,
                               `type`: CandidateType,
                               fullName: String,
                               email: String,
                               links: Seq[Link],
                               realUrl: Option[String],
                               createdAt: DateTime,
                               updatedAt: Option[DateTime])

  implicit val CandidateResponseEncoder: Encoder[CandidateResponse] = deriveEncoder

  def buildResponse(candidate: Candidate): CandidateResponse = CandidateResponse(
    id = candidate.id,
    `type` = candidate.`type`,
    fullName = candidate.fullName,
    email = candidate.email,
    links = candidate.links,
    realUrl = candidate.realUrl,
    createdAt = candidate.createdAt,
    updatedAt = candidate.updatedAt
  )

  case class CandidateRequest(`type`: CandidateType,
                              email: String,
                              fullName: String,
                              links: Seq[Link],
                              realUrl: Option[String],
                              positionId: Option[PositionId] = None,
                              message: Option[String] = None)

  implicit val CandidateRequestDecoder: Decoder[CandidateRequest] = deriveDecoder
  implicit def CandidateRequestEntityDecoder[F[_] : Sync]: EntityDecoder[F, CandidateRequest] =
    jsonOf[F, CandidateRequest]

  def buildCandidate(candidate: CandidateRequest): Candidate = Candidate(
    id = tagUUIDAsCandidateId(UUID.randomUUID()),
    `type` = candidate.`type`,
    fullName = candidate.fullName,
    email = candidate.email,
    links = candidate.links,
    realUrl = candidate.realUrl,
    createdAt = DateTime.now(),
    updatedAt = None,
    isActive = true
  )

  def buildUpdatedCandidate(candidate: Candidate, candidateUpdate: CandidateRequest): Candidate = {
    candidate.copy(
      `type` = candidateUpdate.`type`,
      fullName = candidateUpdate.fullName,
      email = candidate.email,
      links = candidateUpdate.links,
      realUrl = candidateUpdate.realUrl,
      updatedAt = Some(DateTime.now)
    )
  }
}
