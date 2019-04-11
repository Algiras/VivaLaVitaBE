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

class Schema(val profile: JdbcProfile){
  import profile.api._

  implicit val datetimeColumnType: JdbcType[DateTime] with BaseTypedType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime)
  )

  implicit val userIdColumnType: JdbcType[UserId] = MappedColumnType.base[UserId, UUID](
    userId => userId.asInstanceOf[UUID],
    uuId => tagUUIDAsUserId(uuId)
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

  implicit val LinkEncoder: Encoder[Link] = deriveEncoder
  implicit val LinkDecoder: Decoder[Link] = deriveDecoder
  type CandidateLinks = Seq[Link]
  implicit val candidateLinksColumnType: JdbcType[CandidateLinks] = MappedColumnType.base[CandidateLinks, String](
    candidateLinks => candidateLinks.asJson.noSpaces,
    linksStr => parse(linksStr).getOrElse(Json.Null).as[CandidateLinks].getOrElse(Seq.empty)
  )

  class UsersTable(tag: Tag) extends Table[User](tag, "USERS"){
    def id = column[UserId]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL", O.Unique)
    def password = column[Option[String]]("PASSWORD")
    def createdAt = column[DateTime]("CREATED_AT")
    def updatedAt = column[Option[DateTime]]("UPDATED_AT")
    def isActive = column[Boolean]("IS_ACTIVE")

    def * = (id, name, email, password, createdAt, updatedAt, isActive).mapTo[User]
  }

  class PositionTable(tag: Tag) extends Table[Position](tag, "POSITIONS"){
    def id = column[PositionId]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def createdAt = column[DateTime]("CREATED_AT")
    def updatedAt = column[Option[DateTime]]("UPDATED_AT")
    def isActive = column[Boolean]("IS_ACTIVE")

    def * = (id, name, createdAt, updatedAt, isActive).mapTo[Position]
  }

  class MessageTable(tag: Tag) extends Table[Message](tag, "MESSAGES"){
    def id = column[MessageId]("ID", O.PrimaryKey)
    def positionId = column[Option[PositionId]]("POSITION_ID")
    def candidateId = column[Option[CandidateId]]("CANDIDATE_ID")
    def message = column[String]("MESSAGE")
    def createdAt = column[DateTime]("CREATED_AT")
    def updatedAt = column[Option[DateTime]]("UPDATED_AT")
    def isActive = column[Boolean]("IS_ACTIVE")

    def * = (id, positionId, candidateId, message, createdAt, updatedAt, isActive).mapTo[Message]
  }

  class CandidateTable(tag: Tag) extends Table[Candidate](tag, "CANDIDATES"){
    def id = column[CandidateId]("ID", O.PrimaryKey)
    def `type` = column[CandidateType]("TYPE")
    def fullName = column[String]("FULL_NAME")
    def links = column[CandidateLinks]("CANDIDATE_LINKS")
    def realUrl = column[Option[String]]("REAL_URL")
    def createdAt = column[DateTime]("CREATED_AT")
    def updatedAt = column[Option[DateTime]]("UPDATED_AT")
    def isActive = column[Boolean]("IS_ACTIVE")

    def * = (id, `type`, fullName, links, realUrl, createdAt, updatedAt, isActive).mapTo[Candidate]
  }

  val Users: TableQuery[UsersTable] = TableQuery[UsersTable]
  val Positions: TableQuery[PositionTable] = TableQuery[PositionTable]
  val Messages: TableQuery[MessageTable] = TableQuery[MessageTable]
  val Candidates: TableQuery[CandidateTable] = TableQuery[CandidateTable]
}
