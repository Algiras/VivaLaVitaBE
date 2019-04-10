package com.wix.vivaLaVita.database.dao

import cats.~>
import com.wix.vivaLaVita.database.Schema
import com.wix.vivaLaVita.domain.{Message, MessageId}
import slick.dbio
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

trait MessageDAO[F[_]] {
  def create(user: Message): F[Message]

  def update(id: MessageId, user: Message): F[Unit]

  def delete(id: MessageId): F[Unit]

  def read(id: MessageId): F[Option[Message]]

  def paged(page: Int, pageSize: Int): F[Seq[Message]]

  def createSchema(): F[Unit]

  def reset(): F[Unit]
}

object MessageDAO {
  def lift[F[_], G[_]](from: MessageDAO[F])(implicit ev: F ~> G): MessageDAO[G] = new MessageDAO[G] {
    def create(row: Message): G[Message] = ev(from.create(row))
    def read(id: MessageId): G[Option[Message]] = ev(from.read(id))
    def update(id: MessageId, row: Message): G[Unit] = ev(from.update(id, row))
    def delete(id: MessageId): G[Unit] = ev(from.delete(id))

    def createSchema(): G[Unit] = ev(from.createSchema())
    def reset(): G[Unit] = ev(from.reset())

    def paged(page: Int, pageSize: Int): G[Seq[Message]] = ev(from.paged(page, pageSize))
  }
}

class MessageDBIO(val profile: JdbcProfile, schema: Schema)(implicit executionContext: ExecutionContext) extends MessageDAO[dbio.DBIO] {

  import profile.api._
  import schema.{datetimeColumnType, messageIdColumnType}

  override def delete(id: MessageId) = schema.Messages.filter(_.id === id).map(_.isActive).update(false).map(_ => ())

  override def update(id: MessageId, row: Message) = schema.Messages.filter(_.id === id).update(row).map(_ => ())

  override def create(row: Message) = (schema.Messages += row).map(_ => row)

  override def read(id: MessageId) = schema.Messages.filter(row => row.id === id && row.isActive === true).result.headOption

  override def paged(page: Int, pageSize: Int): DBIO[Seq[Message]] = schema.Messages.filter(_.isActive === true).sortBy(_.createdAt).drop(page * pageSize).take(pageSize).result

  override def createSchema(): DBIO[Unit] = schema.Messages.schema.create

  override def reset(): DBIO[Unit] = schema.Messages.delete.map(_ => ())
}

