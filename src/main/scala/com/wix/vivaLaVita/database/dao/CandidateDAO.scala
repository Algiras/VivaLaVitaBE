package com.wix.vivaLaVita.database.dao

import cats.~>
import com.wix.vivaLaVita.database.Schema
import com.wix.vivaLaVita.database.dao.CandidateDAO.Filter
import com.wix.vivaLaVita.domain.{Candidate, CandidateId}
import slick.dbio
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

trait CandidateDAO[F[_]] {
  def create(user: Candidate): F[Candidate]

  def update(id: CandidateId, user: Candidate): F[Unit]

  def delete(id: CandidateId): F[Unit]

  def read(id: CandidateId): F[Option[Candidate]]

  def paged(page: Int, pageSize: Int, filter: Filter = Filter()): F[Seq[Candidate]]

  def createSchema(): F[Unit]

  def reset(): F[Unit]
}

object CandidateDAO {
  case class Filter(fullName: Option[String] = None, link: Option[String] = None, email: Option[String] = None)

  def lift[F[_], G[_]](from: CandidateDAO[F])(implicit ev: F ~> G): CandidateDAO[G] = new CandidateDAO[G] {
    def create(row: Candidate): G[Candidate] = ev(from.create(row))
    def read(id: CandidateId): G[Option[Candidate]] = ev(from.read(id))
    def update(id: CandidateId, row: Candidate): G[Unit] = ev(from.update(id, row))
    def delete(id: CandidateId): G[Unit] = ev(from.delete(id))

    def createSchema(): G[Unit] = ev(from.createSchema())
    def reset(): G[Unit] = ev(from.reset())

    def paged(page: Int, pageSize: Int, filter: Filter = Filter()): G[Seq[Candidate]] = ev(from.paged(page, pageSize, filter))
  }
}

class CandidateDBIO(val profile: JdbcProfile, schema: Schema)(implicit executionContext: ExecutionContext) extends CandidateDAO[dbio.DBIO] {

  import profile.api._
  import schema._

  override def delete(id: CandidateId): DBIO[Unit] = for {
    _ <- schema.Candidates.filter(_.id === id).map(_.isActive).update(false)
    _ <- schema.Links.filter(_.candidateId === id).map(_.isActive).update(false)
  } yield ()

  override def update(id: CandidateId, row: Candidate): DBIO[Unit] = {
    val (candidateNoLink, links) = CandidateHelpers.linkAndCandidate(row)

    for {
      _ <- schema.Candidates.filter(_.id === id).update(candidateNoLink)
      _ <- DBIO.sequence(links.map(lnk => schema.Links.filter(value => value.candidateId === id && value.linkType === lnk.linkType).map(_.url).update(lnk.url)))
    } yield ()
  }

  override def create(row: Candidate): DBIO[Candidate] = {
    val (candidateNoLink, links) = CandidateHelpers.linkAndCandidate(row)

    for {
     _ <- schema.Candidates += candidateNoLink
     _ <- schema.Links ++= links
    } yield row
  }

  override def read(id: CandidateId): DBIO[Option[Candidate]] = for {
    candidate <- schema.Candidates.filter(row => row.id === id && row.isActive === true).result.headOption
    links <- schema.Links.filter(_.candidateId === id).result
  } yield candidate.map(can => CandidateHelpers.toCandidate(can, links))

  override def paged(page: Int, pageSize: Int, filter: Filter = Filter()): DBIO[Seq[Candidate]] = {

    schema.Candidates.filter(row => row.isActive && (filter match {
      case Filter(Some(fullName), Some(link), Some(email)) => row.email === email || row.links.filter(_.url === link).exists || row.fullName === fullName
      case Filter(Some(fullName), None,       Some(email)) => row.email === email ||  row.fullName === fullName
      case Filter(Some(fullName), Some(link), None       ) => row.links.filter(_.url === link).exists || row.fullName === fullName
      case Filter(None,           Some(link), Some(email)) => row.email === email || row.links.filter(_.url === link).exists
      case Filter(Some(fullName), None,       None       ) => row.fullName === fullName
      case Filter(None,           Some(link), None       ) => row.links.filter(_.url === link).exists
      case Filter(None,           None,       Some(email)) => row.email === email
      case _ => LiteralColumn(true)
    })
    )
      .sortBy(_.createdAt)
      .drop(page * pageSize)
      .take(pageSize).result.statements.foreach(println(_))

    for {
      candidates <- schema.Candidates.filter(row => row.isActive && (filter match {
          case Filter(Some(fullName), Some(link), Some(email)) => row.email.like(s"%$email%") || row.links.filter(_.url.like(s"%$link%")).exists || row.fullName.like(s"%$fullName%")
          case Filter(Some(fullName), None,       Some(email)) => row.email.like(s"%$email%") ||  row.fullName.like(s"%$fullName%")
          case Filter(Some(fullName), Some(link), None       ) => row.links.filter(_.url.like(s"%$link%")).exists || row.fullName.like(s"%$fullName%")
          case Filter(None,           Some(link), Some(email)) => row.email.like(s"%$email%") || row.links.filter(_.url.like(s"%$link%")).exists
          case Filter(Some(fullName), None,       None       ) => row.fullName.like(s"%$fullName%")
          case Filter(None,           Some(link), None       ) => row.links.filter(_.url.like(s"%$link%")).exists
          case Filter(None,           None,       Some(email)) => row.email.like(s"%$email%")
          case _ => LiteralColumn(true)
        })
      )
        .sortBy(_.createdAt)
        .drop(page * pageSize)
        .take(pageSize).result
      links <- schema.Links.filter(_.candidateId.inSet(candidates.map(_.id))).result
    } yield {
      val mappedLinks: Map[CandidateId, Seq[schema.CandidateLink]] = links.groupBy(_.candidateId)

      candidates.map(cnd => CandidateHelpers.toCandidate(cnd, mappedLinks(cnd.id)))
    }

  }

  override def createSchema(): DBIO[Unit] = for {
    _ <- schema.Candidates.schema.create
    _ <- schema.Links.schema.create
  } yield ()

  override def reset(): DBIO[Unit] = for {
    _ <- schema.Candidates.delete.map(_ => ())
    _ <- schema.Links.delete.map(_ => ())
  } yield ()
}

