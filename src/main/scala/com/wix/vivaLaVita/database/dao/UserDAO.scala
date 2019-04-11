package com.wix.vivaLaVita.database.dao

import com.wix.vivaLaVita.database.Schema
import com.wix.vivaLaVita.domain.{User, UserId}
import slick.dbio
import slick.jdbc.JdbcProfile
import cats.~>
import tsec.passwordhashers._
import tsec.passwordhashers.jca._

import scala.concurrent.ExecutionContext

trait UserDAO[F[_]] {
  def create(user: User): F[User]

  def update(id: UserId, user: User): F[Unit]

  def delete(id: UserId): F[Unit]

  def read(id: UserId): F[Option[User]]

  def paged(page: Int, pageSize: Int): F[Seq[User]]

  def select(email: String): F[Option[User]]

  def createSchema(): F[Unit]

  def reset(): F[Unit]
}

object UserDAO {
  def lift[F[_], G[_]](from: UserDAO[F])(implicit ev: F ~> G): UserDAO[G] = new UserDAO[G] {
    def create(row: User): G[User] = ev(from.create(row))
    def read(id: UserId): G[Option[User]] = ev(from.read(id))
    def update(id: UserId, row: User): G[Unit] = ev(from.update(id, row))
    def delete(id: UserId): G[Unit] = ev(from.delete(id))

    def createSchema(): G[Unit] = ev(from.createSchema())
    def reset(): G[Unit] = ev(from.reset())

    def paged(page: Int, pageSize: Int): G[Seq[User]] = ev(from.paged(page, pageSize))

    override def select(username: String): G[Option[User]] = ev(from.select(username))
  }
}

class UserDBIO(val profile: JdbcProfile, schema: Schema)(implicit executionContext: ExecutionContext) extends UserDAO[dbio.DBIO] {

  import profile.api._
  import schema.userIdColumnType
  import schema.datetimeColumnType

  override def delete(id: UserId) = schema.Users.filter(_.id === id).map(_.isActive).update(false).map(_ => ())

  override def update(id: UserId, row: User) = schema.Users.filter(_.id === id).update(row).map(_ => ())

  override def create(row: User) = (schema.Users += row).map(_ => row)

  override def read(id: UserId) = schema.Users.filter(row => row.id === id && row.isActive === true).result.headOption

  override def paged(page: Int, pageSize: Int): DBIO[Seq[User]] = schema.Users.filter(_.isActive === true).sortBy(_.createdAt).drop(page * pageSize).take(pageSize).result

  override def createSchema(): DBIO[Unit] = schema.Users.schema.create

  override def reset(): DBIO[Unit] = schema.Users.delete.map(_ => ())

  override def select(email: String): DBIO[Option[User]] = {
    schema.Users.filter(usr => usr.email === email && usr.isActive === true).result.headOption
  }
}

