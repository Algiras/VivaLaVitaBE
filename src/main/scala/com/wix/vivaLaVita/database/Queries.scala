package com.wix.vivaLaVita.database

import cats.effect.{IO, Sync}
import cats.{FlatMap, ~>}
import com.wix.vivaLaVita.database.dao._
import com.wix.vivaLaVita.dto.UserDTO
import com.wix.vivaLaVita.dto.UserDTO.UserRequest
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import cats.syntax.functor._
import cats.syntax.flatMap._
import tsec.passwordhashers._
import tsec.passwordhashers.jca._

import scala.concurrent.ExecutionContext

trait Queries[F[_]] {
  def userDao: UserDAO[F]
}

class DBQueries[F[_]: Sync](profile: JdbcProfile, schema: Schema, transform: DBIO ~> F)(implicit executionContext: ExecutionContext) extends Queries[F] {
  def userDao: UserDAO[F] = UserDAO.lift[DBIO, F](new UserDBIO(profile, schema))(transform)

  def setup: F[Unit] = for {
    _ <- userDao.createSchema()
    hashedPsw <-BCrypt.hashpw[F]("password")
    _ <- userDao.create(UserDTO.buildUser(UserRequest("algimantas", "kras.algim@gmail.com", hashedPsw)))
  } yield ()
}
