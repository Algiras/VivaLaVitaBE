package com.wix.vivaLaVita

import cats.effect._
import cats.implicits._
import cats.~>
import com.wix.vivaLaVita.auth.{AuthService, LoginFlowCheck}
import com.wix.vivaLaVita.config.{Config, UserConfig}
import com.wix.vivaLaVita.database.{DBQueries, Queries, Schema}
import tsec.mac.jca.HMACSHA256
import slick.dbio.DBIO
import pureconfig.generic.auto._
import slick.jdbc.{JdbcBackend, JdbcProfile, PostgresProfile}
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

object App extends IOApp {

  import scala.concurrent.ExecutionContext.Implicits.global

  def queries(jdbcProfile: JdbcProfile, config: UserConfig): IO[Queries[IO]] = {
    // Don't like this..but there is some weird bug in Postgres Setup :(
    val db: JdbcBackend#DatabaseDef = jdbcProfile.backend.Database.forConfig("db")

    val transform: DBIO ~> IO = new (DBIO ~> IO) {
      def apply[T](dbio: DBIO[T]): IO[T] = IO.fromFuture(IO(db.run(dbio)))
    }

    val schema = new Schema(jdbcProfile)
    val queries = new DBQueries[IO](jdbcProfile, schema, config, transform)

    queries.setup.map(_ => queries)
  }

  def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- loadConfigF[IO, Config]
      query <- queries(PostgresProfile, config.admin)
      key <- HMACSHA256.generateKey[IO]
      secureRequestHandler = AuthService[IO](key, query.userDao)
      loginCheck = new LoginFlowCheck[IO](query.userDao, config.google.googleApiKey)
      services <- Server.run[IO](config.http, loginCheck, secureRequestHandler, query)
    } yield services
  }
}
