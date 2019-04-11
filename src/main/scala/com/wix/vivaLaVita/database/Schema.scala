package com.wix.vivaLaVita.database

import java.sql.Timestamp
import java.util.UUID

import com.wix.vivaLaVita.domain.{CandidateType, _}
import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}
import io.circe.syntax._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import slick.lifted.ProvenShape

class Schema(val profile: JdbcProfile) {

  import profile.api._

  implicit val datetimeColumnType: JdbcType[DateTime] with BaseTypedType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime)
  )

  implicit val userIdColumnType: JdbcType[UserId] = MappedColumnType.base[UserId, UUID](
    userId => userId.asInstanceOf[UUID],
    uuId => tagUUIDAsUserId(uuId)
  )

  implicit val hiringProcessIdColumnType: JdbcType[HiringProcessId] = MappedColumnType.base[HiringProcessId, UUID](
    hiringProcessId => hiringProcessId.asInstanceOf[UUID],
    uuId => tagUUIDAsHiringProcessId(uuId)
  )

  implicit val positionIdColumnType: JdbcType[PositionId] = MappedColumnType.base[PositionId, UUID](
    positionId => positionId.asInstanceOf[UUID],
    uuId => tagUUIDAsPositionId(uuId)
  )

  implicit val messageIdColumnType: JdbcType[MessageId] = MappedColumnType.base[MessageId, UUID](
    messageId => messageId.asInstanceOf[UUID],
    uuId => tagUUIDAsMessageId(uuId)
  )

  implicit val candidateIdColumnType: JdbcType[CandidateId] = MappedColumnType.base[CandidateId, UUID](
    candidateId => candidateId.asInstanceOf[UUID],
    uuId => tagUUIDAsCandidateId(uuId)
  )

  implicit val candidateTypeColumnType: JdbcType[CandidateType] = MappedColumnType.base[CandidateType, String](
    candidateType => candidateType.entryName,
    name => CandidateType.namesToValuesMap(name)
  )

  implicit val candidateLinkTypeColumnType: JdbcType[LinkType] = MappedColumnType.base[LinkType, String](
    linkType => linkType.entryName,
    name => LinkType.namesToValuesMap(name)
  )

  implicit val hiringProcessStatusColumnType: JdbcType[HiringProcessStatusType] = MappedColumnType.base[HiringProcessStatusType, String](
    hiringProcessStatus => hiringProcessStatus.entryName,
    name => HiringProcessStatusType.namesToValuesMap(name)
  )


  class UsersTable(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[UserId]("ID", O.PrimaryKey)

    def name = column[String]("NAME")

    def email = column[String]("EMAIL", O.Unique)

    def password = column[Option[String]]("PASSWORD")

    def createdAt = column[DateTime]("CREATED_AT")

    def updatedAt = column[Option[DateTime]]("UPDATED_AT")

    def isActive = column[Boolean]("IS_ACTIVE")

    def * = (id, name, email, password, createdAt, updatedAt, isActive).mapTo[User]
  }

  class PositionTable(tag: Tag) extends Table[Position](tag, "POSITIONS") {
    def id = column[PositionId]("ID", O.PrimaryKey)

    def name = column[String]("NAME")

    def createdAt = column[DateTime]("CREATED_AT")

    def updatedAt = column[Option[DateTime]]("UPDATED_AT")

    def isActive = column[Boolean]("IS_ACTIVE")

    def * = (id, name, createdAt, updatedAt, isActive).mapTo[Position]
  }

  class HiringProcessTable(tag: Tag) extends Table[HiringProcess](tag, "HIRING_PROCESS") {
    def id = column[HiringProcessId]("ID")

    def candidateId = column[CandidateId]("CANDIDATE_ID")

    def positionId = column[PositionId]("POSITION_ID")

    def status = column[HiringProcessStatusType]("HIRING_PROCESS_STATUS")

    def createdAt = column[DateTime]("CREATED_AT")

    def updatedAt = column[Option[DateTime]]("UPDATED_AT")

    def isActive = column[Boolean]("IS_ACTIVE")

    def pk = primaryKey("pk_hiringProcess", (candidateId, positionId))

    def * = (id, candidateId, positionId, status, createdAt, updatedAt, isActive).mapTo[HiringProcess]
  }

  class MessageTable(tag: Tag) extends Table[Message](tag, "MESSAGES") {
    def id = column[MessageId]("ID", O.PrimaryKey)

    def positionId = column[Option[PositionId]]("POSITION_ID")

    def candidateId = column[Option[CandidateId]]("CANDIDATE_ID")

    def message = column[String]("MESSAGE")

    def createdAt = column[DateTime]("CREATED_AT")

    def updatedAt = column[Option[DateTime]]("UPDATED_AT")

    def isActive = column[Boolean]("IS_ACTIVE")

    def * = (id, positionId, candidateId, message, createdAt, updatedAt, isActive).mapTo[Message]
  }

  case class CandidateLink(candidateId: CandidateId, linkType: LinkType, url: String, isActive: Boolean)

  class CandidateLinkTable(tag: Tag) extends Table[CandidateLink](tag, "LINKS") {
    def candidateId = column[CandidateId]("CANDIDATE_ID")

    def linkType = column[LinkType]("LINK_TYPE")

    def url = column[String]("URL")

    def isActive = column[Boolean]("IS_ACTIVE")

    def candidate = foreignKey("candidate_fk", candidateId, Candidates)(_.id)

    def pk = primaryKey("pk_a", (candidateId, linkType))

    def * = (candidateId, linkType, url, isActive).mapTo[CandidateLink]
  }


  case class CandidateNoLinks(id: CandidateId,
                              `type`: CandidateType,
                              fullName: String,
                              email: String,
                              realUrl: Option[String],
                              createdAt: DateTime,
                              updatedAt: Option[DateTime],
                              isActive: Boolean)

  object CandidateHelpers {
    def linkAndCandidate(candidate: Candidate): (CandidateNoLinks, Seq[CandidateLink]) = {
      import candidate._
      (
        CandidateNoLinks(id = id, `type` = `type`, fullName = fullName, email = email, realUrl = realUrl, createdAt = createdAt, updatedAt = updatedAt, isActive = isActive),
        links.map(lnk => CandidateLink(id, lnk.linkType, lnk.url, isActive))
      )
    }

    def toCandidate(candidate: CandidateNoLinks, links: Seq[CandidateLink]) = Candidate(
      id = candidate.id,
      `type` = candidate.`type`,
      email = candidate.email,
      fullName = candidate.fullName,
      links = links.map(lnk => Link(lnk.linkType, lnk.url)),
      realUrl = candidate.realUrl,
      createdAt = candidate.createdAt,
      updatedAt = candidate.updatedAt,
      isActive = candidate.isActive
    )
  }

  class CandidateTable(tag: Tag) extends Table[CandidateNoLinks](tag, "CANDIDATES") {
    def id: Rep[CandidateId] = column[CandidateId]("ID", O.PrimaryKey)

    def `type`: Rep[CandidateType] = column[CandidateType]("TYPE")

    def fullName: Rep[String] = column[String]("FULL_NAME")

    def email: Rep[String] = column[String]("EMAIL")

    def realUrl: Rep[Option[String]] = column[Option[String]]("REAL_URL")

    def createdAt: Rep[DateTime] = column[DateTime]("CREATED_AT")

    def updatedAt: Rep[Option[DateTime]] = column[Option[DateTime]]("UPDATED_AT")

    def isActive: Rep[Boolean] = column[Boolean]("IS_ACTIVE")

    def links = Links.filter(_.candidateId === id)

    def * = (id, `type`, fullName, email, realUrl, createdAt, updatedAt, isActive).mapTo[CandidateNoLinks]
  }

  val Users: TableQuery[UsersTable] = TableQuery[UsersTable]
  val Positions: TableQuery[PositionTable] = TableQuery[PositionTable]
  val Messages: TableQuery[MessageTable] = TableQuery[MessageTable]
  val Candidates: TableQuery[CandidateTable] = TableQuery[CandidateTable]
  val Links: TableQuery[CandidateLinkTable] = TableQuery[CandidateLinkTable]
  val HiringProcess: TableQuery[HiringProcessTable] = TableQuery[HiringProcessTable]
}
