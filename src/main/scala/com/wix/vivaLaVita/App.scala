package com.wix.vivaLaVita

import cats.effect._
import cats.implicits._
import cats.~>
import com.wix.vivaLaVita.auth.{AuthService, LoginFlowCheck}
import com.wix.vivaLaVita.config.{Config, DBConfig}
import com.wix.vivaLaVita.database.{DBQueries, Queries, Schema}
import tsec.mac.jca.HMACSHA256
import slick.dbio.DBIO
import pureconfig.generic.auto._
import slick.jdbc.{H2Profile, JdbcBackend, JdbcProfile}
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

object App extends IOApp {

  import scala.concurrent.ExecutionContext.Implicits.global

  def queries(jdbcProfile: JdbcProfile, config: DBConfig): IO[Queries[IO]] = {
    val db: JdbcBackend#DatabaseDef = jdbcProfile.backend.Database.forURL(
      config.url,
      driver = config.driver,
      keepAliveConnection = config.keepAliveConnection)

    val transform: DBIO ~> IO = new (DBIO ~> IO) {
      def apply[T](dbio: DBIO[T]): IO[T] = IO.fromFuture(IO(db.run(dbio)))
    }

    val schema = new Schema(jdbcProfile)
    val dbQueries: DBQueries[IO] = new DBQueries[IO](jdbcProfile, schema, transform)

    dbQueries.setup.map(_ => dbQueries)
  }

  def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- loadConfigF[IO, Config]
      query <- queries(H2Profile, config.db)
      key <- HMACSHA256.generateKey[IO]
      secureRequestHandler = AuthService[IO](key, query.userDao)
      loginCheck = new LoginFlowCheck[IO](query.userDao, config.google.googleApiKey)
      services <- Server.run[IO](config.http, loginCheck, secureRequestHandler, query)
    } yield services
  }
}
