package com.wix.vivaLaVita.database.dao

import cats.~>
import com.wix.vivaLaVita.database.Schema
import com.wix.vivaLaVita.domain.{Position, PositionId}
import slick.dbio
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

trait PositionDAO[F[_]] {
  def create(user: Position): F[Position]

  def update(id: PositionId, user: Position): F[Unit]

  def delete(id: PositionId): F[Unit]

  def read(id: PositionId): F[Option[Position]]

  def paged(page: Int, pageSize: Int): F[Seq[Position]]

  def createSchema(): F[Unit]

  def reset(): F[Unit]
}

object PositionDAO {
  def lift[F[_], G[_]](from: PositionDAO[F])(implicit ev: F ~> G): PositionDAO[G] = new PositionDAO[G] {
    def create(row: Position): G[Position] = ev(from.create(row))
    def read(id: PositionId): G[Option[Position]] = ev(from.read(id))
    def update(id: PositionId, row: Position): G[Unit] = ev(from.update(id, row))
    def delete(id: PositionId): G[Unit] = ev(from.delete(id))

    def createSchema(): G[Unit] = ev(from.createSchema())
    def reset(): G[Unit] = ev(from.reset())

    def paged(page: Int, pageSize: Int): G[Seq[Position]] = ev(from.paged(page, pageSize))
  }
}

class PositionDBIO(val profile: JdbcProfile, schema: Schema)(implicit executionContext: ExecutionContext) extends PositionDAO[dbio.DBIO] {

  import profile.api._
  import schema.{datetimeColumnType, positionIdColumnType}

  override def delete(id: PositionId) = schema.Positions.filter(_.id === id).map(_.isActive).update(false).map(_ => ())

  override def update(id: PositionId, row: Position) = schema.Positions.filter(_.id === id).update(row).map(_ => ())

  override def create(row: Position) = (schema.Positions += row).map(_ => row)

  override def read(id: PositionId) = schema.Positions.filter(row => row.id === id && row.isActive === true).result.headOption

  override def paged(page: Int, pageSize: Int): DBIO[Seq[Position]] = schema.Positions.filter(_.isActive === true).sortBy(_.createdAt).drop(page * pageSize).take(pageSize).result

  override def createSchema(): DBIO[Unit] = schema.Positions.schema.create

  override def reset(): DBIO[Unit] = schema.Positions.delete.map(_ => ())
}

