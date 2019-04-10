package com.wix.vivaLaVita.database

import java.sql.Timestamp
import java.util.UUID

import com.wix.vivaLaVita.domain.{User, UserId, tagUUIDAsUserId}
import org.joda.time.DateTime
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

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

  class UsersTable(tag: Tag) extends Table[User](tag, "USERS"){
    def id = column[UserId]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")
    def password = column[String]("PASSWORD")
    def createdAt = column[DateTime]("CREATED_AT")
    def updatedAt = column[Option[DateTime]]("UPDATED_AT")
    def isActive = column[Boolean]("IS_ACTIVE")

    def * = (id, name, email, password, createdAt, updatedAt, isActive).mapTo[User]
  }

  val Users: TableQuery[UsersTable] = TableQuery[UsersTable]
}
