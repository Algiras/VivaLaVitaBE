package com.wix.vivaLaVita

import shapeless.tag
import java.util.UUID
import com.wix.vivaLaVita.database.dao.Identifiable
import org.joda.time.DateTime
import shapeless.tag.@@

package object domain {

  trait UserIdTag
  trait PositionIdTag
  trait CandidateIdTag
  trait MessageIdTag

  type UserId = UUID @@ UserIdTag
  type PositionId = UUID @@ PositionIdTag
  type CandidateId = UUID @@ CandidateIdTag
  type MessageId = UUID @@ MessageIdTag

  def tagUUIDAsUserId(id: UUID): UserId = tag[UserIdTag][UUID](id)
  def tagUUIDAsPositionId(id: UUID): PositionId = tag[PositionIdTag][UUID](id)
  def tagUUIDAsCandidateId(id: UUID): CandidateId = tag[CandidateIdTag][UUID](id)
  def tagUUIDAsMessageId(id: UUID): MessageId = tag[MessageIdTag][UUID](id)

  case class User(id: UserId,
                  name: String,
                  email: String,
                  password: String,
                  createdAt: DateTime,
                  updatedAt: Option[DateTime],
                  isActive: Boolean) extends Identifiable[UserId, User] {
    override def getId(value: User): UserId = id
  }

  case class Position(id: PositionId,
                      name: String,
                      createdAt: DateTime,
                      updatedAt: Option[DateTime],
                      isActive: Boolean) extends Identifiable[PositionId, Position] {
    override def getId(value: Position): PositionId = id
  }

  case class Message(id: MessageId,
                     positionId: Option[PositionId],
                     candidateId: Option[CandidateId],
                     message: String,
                     createdAt: DateTime,
                     updatedAt: Option[DateTime],
                     isActive: Boolean) extends Identifiable[MessageId, Message] {
    override def getId(value: Message): MessageId = id
  }
}
