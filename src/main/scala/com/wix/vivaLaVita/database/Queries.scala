package com.wix.vivaLaVita.database

import cats.effect.Sync
import cats.~>
import com.wix.vivaLaVita.database.dao._
import com.wix.vivaLaVita.dto.UserDTO
import com.wix.vivaLaVita.dto.UserDTO.UserRequest
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import tsec.passwordhashers.jca._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.instances.vector._

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
    tableNames <- transform(MTable.getTables.map(tables => tables.map(_.name.name)))
    _ <- tableNames.find(_ == schema.Users.baseTableRow.tableName).map(_ => Sync[F].pure(())).getOrElse(userDao.createSchema())
    _ <- tableNames.find(_ == schema.Positions.baseTableRow.tableName).map(_ => Sync[F].pure(())).getOrElse(positionDao.createSchema())
    _ <- tableNames.find(_ == schema.Messages.baseTableRow.tableName).map(_ => Sync[F].pure(())).getOrElse(messageDao.createSchema())
    _ <- tableNames.find(_ == schema.Candidates.baseTableRow.tableName).map(_ => Sync[F].pure(())).getOrElse(candidateDao.createSchema())
    _ <- tableNames.find(_ == schema.HiringProcess.baseTableRow.tableName).map(_ => Sync[F].pure(())).getOrElse(hiringProcessDao.createSchema())
    //
    _ <- userDao.select("kras.algim@gmail.com").flatMap(usr => if(usr.isDefined) {
      Sync[F].pure(())
    } else {
      BCrypt.hashpw[F]("password").flatMap(psw => userDao.create(UserDTO.buildUser(UserRequest("algimantas", "kras.algim@gmail.com", Some(psw))))).map(_ => ())
    })
  } yield ()
}
