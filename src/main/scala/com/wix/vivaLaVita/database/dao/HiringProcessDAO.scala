package com.wix.vivaLaVita.database.dao

import cats.~>
import com.wix.vivaLaVita.database.Schema
import com.wix.vivaLaVita.domain.{HiringProcess, HiringProcessId}
import slick.dbio
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

trait HiringProcessDAO[F[_]] {
  def create(hiringProcess: HiringProcess): F[HiringProcess]

  def update(id: HiringProcessId, hiringProcess: HiringProcess): F[Unit]

  def delete(id: HiringProcessId): F[Unit]

  def read(id: HiringProcessId): F[Option[HiringProcess]]

  def paged(page: Int, pageSize: Int): F[Seq[HiringProcess]]

  def createSchema(): F[Unit]

  def reset(): F[Unit]
}

object HiringProcessDAO {
  def lift[F[_], G[_]](from: HiringProcessDAO[F])(implicit ev: F ~> G): HiringProcessDAO[G] = new HiringProcessDAO[G] {
    def create(hiringProcess: HiringProcess): G[HiringProcess] = ev(from.create(hiringProcess))
    def read(id: HiringProcessId): G[Option[HiringProcess]] = ev(from.read(id))
    def update(id: HiringProcessId, hiringProcess: HiringProcess): G[Unit] = ev(from.update(id, hiringProcess))
    def delete(id: HiringProcessId): G[Unit] = ev(from.delete(id))

    def createSchema(): G[Unit] = ev(from.createSchema())
    def reset(): G[Unit] = ev(from.reset())

    def paged(page: Int, pageSize: Int): G[Seq[HiringProcess]] = ev(from.paged(page, pageSize))
  }
}

class HiringProcessDBIO(val profile: JdbcProfile, schema: Schema)(implicit executionContext: ExecutionContext) extends HiringProcessDAO[dbio.DBIO] {

  import profile.api._
  import schema.{datetimeColumnType, hiringProcessIdColumnType}

  override def delete(id: HiringProcessId) = schema.HiringProcess.filter(_.id === id).map(_.isActive).update(false).map(_ => ())

  override def update(id: HiringProcessId, row: HiringProcess) = schema.HiringProcess.filter(_.id === id).update(row).map(_ => ())

  override def create(row: HiringProcess) = (schema.HiringProcess += row).map(_ => row)

  override def read(id: HiringProcessId) = schema.HiringProcess.filter(row => row.id === id && row.isActive === true).result.headOption

  override def paged(page: Int, pageSize: Int): DBIO[Seq[HiringProcess]] = schema.HiringProcess.filter(_.isActive === true).sortBy(_.createdAt).drop(page * pageSize).take(pageSize).result

  override def createSchema(): DBIO[Unit] = schema.HiringProcess.schema.create

  override def reset(): DBIO[Unit] = schema.HiringProcess.delete.map(_ => ())
}

