package com.wix.vivaLaVita.dto

import java.util.UUID

import cats.effect.Sync
import com.wix.vivaLaVita.domain.{CandidateId, _}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime
import shapeless.tag

object CandidateDTO {
  case class CandidateResponse(id: CandidateId,
                               `type`: CandidateType,
                               fullName: String,
                               links: Seq[RequestLink],
                               realUrl: Option[String],
                               createdAt: DateTime,
                               updatedAt: Option[DateTime])

  implicit val CandidateResponseEncoder: Encoder[CandidateResponse] = deriveEncoder

  def buildResponse(candidate: Candidate): CandidateResponse = CandidateResponse(
    id = candidate.id,
    `type` = candidate.`type`,
    fullName = candidate.fullName,
    links = candidate.links.map(link => RequestLink(link.linkType, link.url)),
    realUrl = candidate.realUrl,
    createdAt = candidate.createdAt,
    updatedAt = candidate.updatedAt
  )

  case class CandidateRequest(`type`: CandidateType,
                              fullName: String,
                              links: Seq[RequestLink],
                              realUrl: Option[String])

  implicit val CandidateRequestDecoder: Decoder[CandidateRequest] = deriveDecoder
  implicit def CandidateRequestEntityDecoder[F[_] : Sync]: EntityDecoder[F, CandidateRequest] =
    jsonOf[F, CandidateRequest]

  def buildCandidate(candidate: CandidateRequest, links: Seq[Link]): Candidate = Candidate(
    id = tagUUIDAsCandidateId(UUID.randomUUID()),
    `type` = candidate.`type`,
    fullName = candidate.fullName,
    links = links,
    realUrl = candidate.realUrl,
    createdAt = DateTime.now(),
    updatedAt = None,
    isActive = true
  )

  def buildUpdatedCandidate(candidate: Candidate, CandidateUpdate: CandidateRequest): Candidate = {
    candidate.copy(
      `type` = candidate.`type`,
      fullName = candidate.fullName,
      links = candidate.links.map(rLink => Link(candidate.id, rLink.linkType, rLink.url)),
      realUrl = candidate.realUrl,
      updatedAt = Some(DateTime.now)
    )
  }
}
