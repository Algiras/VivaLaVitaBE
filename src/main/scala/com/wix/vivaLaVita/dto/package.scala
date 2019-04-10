package com.wix.vivaLaVita

import java.util.UUID

import com.wix.vivaLaVita.domain._
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto.deriveDecoder
import org.joda.time.{DateTime, DateTimeZone}

package object dto {
  implicit val dateEncoder: Encoder[DateTime] = (a: DateTime) => Json.fromLong(a.getMillis)

  implicit val dateDecoder: Decoder[DateTime] = (c: HCursor) => c.value.asNumber.flatMap(_.toLong) match {
    case Some(nr) => Right(new DateTime(nr, DateTimeZone.UTC))
    case None => Left(DecodingFailure("DateTime", c.history))
  }

  implicit val userIdEncoder: Encoder[UserId] = (id: UserId) => Encoder[UUID](Encoder[UUID])(id.asInstanceOf[UUID])
  implicit val userIdDecoder: Decoder[UserId] = (c: HCursor) => Decoder[UUID](Decoder[UUID])(c).map(tagUUIDAsUserId)

  implicit val positionIdEncoder: Encoder[PositionId] = (id: PositionId) => Encoder[UUID](Encoder[UUID])(id.asInstanceOf[UUID])
  implicit val positionIdDecoder: Decoder[PositionId] = (c: HCursor) => Decoder[UUID](Decoder[UUID])(c).map(tagUUIDAsPositionId)

  implicit val messageIdEncoder: Encoder[MessageId] = (id: MessageId) => Encoder[UUID](Encoder[UUID])(id.asInstanceOf[UUID])
  implicit val messageIdDecoder: Decoder[MessageId] = (c: HCursor) => Decoder[UUID](Decoder[UUID])(c).map(tagUUIDAsMessageId)

  implicit val candidateIdEncoder: Encoder[CandidateId] = (id: CandidateId) => Encoder[UUID](Encoder[UUID])(id.asInstanceOf[UUID])
  implicit val candidateIdDecoder: Decoder[CandidateId] = (c: HCursor) => Decoder[UUID](Decoder[UUID])(c).map(tagUUIDAsCandidateId)
}
