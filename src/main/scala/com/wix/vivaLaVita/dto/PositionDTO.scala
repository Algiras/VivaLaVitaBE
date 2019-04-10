package com.wix.vivaLaVita.dto

import java.util.UUID

import com.wix.vivaLaVita.domain.PositionId
import cats.effect.Sync
import com.wix.vivaLaVita.domain._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime
import shapeless.tag

object PositionDTO {

  case class PositionResponse(id: PositionId, name: String, createdAt: DateTime, updatedAt: Option[DateTime])
  implicit val PositionResponseEncoder: Encoder[PositionResponse] = deriveEncoder

  def buildResponse(position: Position): PositionResponse = PositionResponse(
    id = position.id,
    name = position.name,
    createdAt = position.createdAt,
    updatedAt = position.updatedAt
  )

  case class PositionRequest(name: String)
  implicit val PositionRequestDecoder: Decoder[PositionRequest] = deriveDecoder
  implicit def PositionRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, PositionRequest] =
    jsonOf[F, PositionRequest]

  def buildPosition(position: PositionRequest): Position = Position(
    id = tagUUIDAsPositionId(UUID.randomUUID()),
    name = position.name,
    createdAt = DateTime.now(),
    updatedAt = None,
    isActive = true
  )

  def buildUpdatedPosition(position: Position, positionUpdate: PositionRequest): Position = {
    position.copy(
      name = positionUpdate.name,
      updatedAt = Some(DateTime.now)
    )
  }
}
