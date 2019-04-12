package com.wix.vivaLaVita

import cats.effect._
import cats.implicits._
import cats.~>
import com.wix.vivaLaVita.auth.{AuthService, LoginFlowCheck}
import com.wix.vivaLaVita.config.Config
import com.wix.vivaLaVita.database.{DBQueries, Queries, Schema}
import tsec.mac.jca.HMACSHA256
import slick.dbio.DBIO
import pureconfig.generic.auto._
import slick.jdbc.{JdbcBackend, JdbcProfile, PostgresProfile}
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

object App extends IOApp {

  import scala.concurrent.ExecutionContext.Implicits.global

  def queries(jdbcProfile: JdbcProfile): DBQueries[IO] = {
    // Don't like this..but there is some weird bug in Postgres Setup :(
    val db: JdbcBackend#DatabaseDef = jdbcProfile.backend.Database.forConfig("db")

    val transform: DBIO ~> IO = new (DBIO ~> IO) {
      def apply[T](dbio: DBIO[T]): IO[T] = IO.fromFuture(IO(db.run(dbio)))
    }

    val schema = new Schema(jdbcProfile)

    new DBQueries[IO](jdbcProfile, schema, transform)
  }

  def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- loadConfigF[IO, Config]
      query <- IO(queries(PostgresProfile))
      key <- HMACSHA256.generateKey[IO]
      secureRequestHandler = AuthService[IO](key, query.userDao)
      loginCheck = new LoginFlowCheck[IO](query.userDao, config.google.googleApiKey)
      services <- Server.run[IO](config.http, loginCheck, secureRequestHandler, query)
    } yield services
  }
}
