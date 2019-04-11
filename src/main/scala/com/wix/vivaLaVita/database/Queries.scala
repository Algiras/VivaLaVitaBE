package com.wix.vivaLaVita.database

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.~>
import com.wix.vivaLaVita.database.dao._
import com.wix.vivaLaVita.domain.{CandidateType, HiringProcessStatusType, Link, LinkType}
import com.wix.vivaLaVita.dto.CandidateDTO.CandidateRequest
import com.wix.vivaLaVita.dto.HiringProcessDTO.HiringProcessRequest
import com.wix.vivaLaVita.dto.MessageDTO.MessageRequest
import com.wix.vivaLaVita.dto.PositionDTO.PositionRequest
import com.wix.vivaLaVita.dto.{CandidateDTO, HiringProcessDTO, MessageDTO, PositionDTO, UserDTO}
import com.wix.vivaLaVita.dto.UserDTO.UserRequest
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import tsec.passwordhashers.jca._

import scala.concurrent.ExecutionContext

trait Queries[F[_]] {
  def userDao: UserDAO[F]
  def positionDao: PositionDAO[F]
  def messageDao: MessageDAO[F]
  def candidateDao: CandidateDAO[F]
  def hiringProcessDao: HiringProcessDAO[F]
}

class DBQueries[F[_]: Sync](profile: JdbcProfile, schema: Schema, transform: DBIO ~> F)(implicit executionContext: ExecutionContext) extends Queries[F] {
  def userDao: UserDAO[F] = UserDAO.lift[DBIO, F](new UserDBIO(profile, schema))(transform)
  def positionDao: PositionDAO[F] = PositionDAO.lift[DBIO, F](new PositionDBIO(profile, schema))(transform)
  def messageDao: MessageDAO[F] = MessageDAO.lift[DBIO, F](new MessageDBIO(profile, schema))(transform)
  def candidateDao: CandidateDAO[F] = CandidateDAO.lift[DBIO, F](new CandidateDBIO(profile, schema))(transform)
  def hiringProcessDao: HiringProcessDAO[F] = HiringProcessDAO.lift[DBIO, F](new HiringProcessDBIO(profile, schema))(transform)

  def setup: F[Unit] = for {
    _ <- userDao.createSchema()
    _ <- positionDao.createSchema()
    _ <- messageDao.createSchema()
    _ <- candidateDao.createSchema()
    _ <- hiringProcessDao.createSchema()
    _ <- BCrypt.hashpw[F]("password").flatMap(psw => userDao.create(UserDTO.buildUser(UserRequest("algimantas", "kras.algim@gmail.com", Some(psw)))))
    _ <- positionDao.create(PositionDTO.buildPosition(PositionRequest("Awesome UX Developer")))
    _ <- positionDao.create(PositionDTO.buildPosition(PositionRequest("Awesome FE Developer")))
    position <- positionDao.create(PositionDTO.buildPosition(PositionRequest("Awesome BE Developer")))
    candidate <- candidateDao.create(
      CandidateDTO.buildCandidate(CandidateRequest(`type` = CandidateType.Lead, email = "email@email.com", fullName = "Mindaugas", links = Seq(Link(LinkType.LinkedIn, "http://LinkedIn")), realUrl = None))
    )
    _ <- hiringProcessDao.create(HiringProcessDTO.buildHiringProcess(HiringProcessRequest(position.id, candidate.id, HiringProcessStatusType.Active)))
    _ <- messageDao.create(MessageDTO.buildMessage(MessageRequest(Some(position.id), None, "Some message")))
  } yield ()
}
