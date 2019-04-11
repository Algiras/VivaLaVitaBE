package com.wix.vivaLaVita.dto

import java.util.UUID

import cats.effect.Sync
import com.wix.vivaLaVita.domain.{MessageId, _}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime
import shapeless.tag

object MessageDTO {
  case class MessageResponse(id: MessageId,
                             positionId: Option[PositionId],
                             candidateId: Option[CandidateId],
                             message: String,
                             createdAt: DateTime,
                             updatedAt: Option[DateTime])

  implicit val MessageResponseEncoder: Encoder[MessageResponse] = deriveEncoder

  def buildResponse(msg: Message): MessageResponse = MessageResponse(
    id = msg.id,
    positionId = msg.positionId,
    candidateId = msg.candidateId,
    message = msg.message,
    createdAt = msg.createdAt,
    updatedAt = msg.updatedAt
  )

  case class MessageRequest(positionId: Option[PositionId],
                            candidateId: Option[CandidateId],
                            message: String)

  implicit val MessageRequestDecoder: Decoder[MessageRequest] = deriveDecoder
  implicit def MessageRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, MessageRequest] =
    jsonOf[F, MessageRequest]

  def buildMessage(msg: MessageRequest): Message = Message(
    id = tagUUIDAsMessageId(UUID.randomUUID()),
    positionId = msg.positionId,
    candidateId = msg.candidateId,
    message = msg.message,
    createdAt = DateTime.now(),
    updatedAt = None,
    isActive = true
  )

  def buildUpdatedMessage(msg: Message, messageUpdate: MessageRequest): Message = {
    msg.copy(
      positionId = messageUpdate.positionId,
      candidateId = messageUpdate.candidateId,
      message = messageUpdate.message,
      updatedAt = Some(DateTime.now)
    )
  }
}
