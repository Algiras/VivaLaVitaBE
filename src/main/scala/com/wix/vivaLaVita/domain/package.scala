package com.wix.vivaLaVita

import shapeless.tag
import java.util.UUID
import org.joda.time.DateTime
import shapeless.tag.@@

package object domain {

  trait UserIdTag
  trait PositionIdTag
  trait CandidateIdTag
  trait MessageIdTag
  trait HiringProcessIdTag

  type UserId = UUID @@ UserIdTag
  type PositionId = UUID @@ PositionIdTag
  type CandidateId = UUID @@ CandidateIdTag
  type MessageId = UUID @@ MessageIdTag
  type HiringProcessId = UUID @@ HiringProcessIdTag

  def tagUUIDAsUserId(id: UUID): UserId = tag[UserIdTag][UUID](id)
  def tagUUIDAsPositionId(id: UUID): PositionId = tag[PositionIdTag][UUID](id)
  def tagUUIDAsCandidateId(id: UUID): CandidateId = tag[CandidateIdTag][UUID](id)
  def tagUUIDAsMessageId(id: UUID): MessageId = tag[MessageIdTag][UUID](id)
  def tagUUIDAsHiringProcessId(id: UUID): HiringProcessId = tag[HiringProcessIdTag][UUID](id)

  case class User(id: UserId,
                  name: String,
                  email: String,
                  password: Option[String],
                  createdAt: DateTime,
                  updatedAt: Option[DateTime],
                  isActive: Boolean)

  case class HiringProcess(id: HiringProcessId,
                           candidateId: CandidateId,
                           positionId: PositionId,
                           status: HiringProcessStatus,
                           createdAt: DateTime,
                           updatedAt: Option[DateTime],
                           isActive: Boolean)

  case class Position(id: PositionId,
                      name: String,
                      createdAt: DateTime,
                      updatedAt: Option[DateTime],
                      isActive: Boolean)

  case class Message(id: MessageId,
                     positionId: Option[PositionId],
                     candidateId: Option[CandidateId],
                     message: String,
                     createdAt: DateTime,
                     updatedAt: Option[DateTime],
                     isActive: Boolean)

  case class Link(linkType: LinkType, url: String)

  case class Candidate(id: CandidateId,
                       `type`: CandidateType,
                       email: String,
                       fullName: String,
                       links: Seq[Link],
                       realUrl: Option[String],
                       createdAt: DateTime,
                       updatedAt: Option[DateTime],
                       isActive: Boolean)
}
