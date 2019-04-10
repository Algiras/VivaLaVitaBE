package com.wix.vivaLaVita.database.dao

import cats.~>
import com.wix.vivaLaVita.database.Schema
import com.wix.vivaLaVita.domain.{Candidate, CandidateId}
import slick.dbio
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

trait CandidateDAO[F[_]] {
  def create(user: Candidate): F[Candidate]

  def update(id: CandidateId, user: Candidate): F[Unit]

  def delete(id: CandidateId): F[Unit]

  def read(id: CandidateId): F[Option[Candidate]]

  def paged(page: Int, pageSize: Int): F[Seq[Candidate]]

  def createSchema(): F[Unit]

  def reset(): F[Unit]
}

object CandidateDAO {
  def lift[F[_], G[_]](from: CandidateDAO[F])(implicit ev: F ~> G): CandidateDAO[G] = new CandidateDAO[G] {
    def create(row: Candidate): G[Candidate] = ev(from.create(row))
    def read(id: CandidateId): G[Option[Candidate]] = ev(from.read(id))
    def update(id: CandidateId, row: Candidate): G[Unit] = ev(from.update(id, row))
    def delete(id: CandidateId): G[Unit] = ev(from.delete(id))

    def createSchema(): G[Unit] = ev(from.createSchema())
    def reset(): G[Unit] = ev(from.reset())

    def paged(page: Int, pageSize: Int): G[Seq[Candidate]] = ev(from.paged(page, pageSize))
  }
}

class CandidateDBIO(val profile: JdbcProfile, schema: Schema)(implicit executionContext: ExecutionContext) extends CandidateDAO[dbio.DBIO] {

  import profile.api._
  import schema.{datetimeColumnType, candidateIdColumnType}

  override def delete(id: CandidateId) = schema.Candidates.filter(_.id === id).map(_.isActive).update(false).map(_ => ())

  override def update(id: CandidateId, row: Candidate) = schema.Candidates.filter(_.id === id).update(row).map(_ => ())

  override def create(row: Candidate) = (schema.Candidates += row).map(_ => row)

  override def read(id: CandidateId) = schema.Candidates.filter(row => row.id === id && row.isActive === true).result.headOption

  override def paged(page: Int, pageSize: Int): DBIO[Seq[Candidate]] = schema.Candidates.filter(_.isActive === true).sortBy(_.createdAt).drop(page * pageSize).take(pageSize).result

  override def createSchema(): DBIO[Unit] = schema.Candidates.schema.create

  override def reset(): DBIO[Unit] = schema.Candidates.delete.map(_ => ())
}

