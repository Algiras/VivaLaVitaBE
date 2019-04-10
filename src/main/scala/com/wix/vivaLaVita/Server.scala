package com.wix.vivaLaVita

import java.util.UUID

import cats.effect._
import cats.implicits._
import com.wix.vivaLaVita.config.HttpConfig
import com.wix.vivaLaVita.database.Queries
import com.wix.vivaLaVita.domain.{User, UserId}
import com.wix.vivaLaVita.service.{LoginService, StaticsService, UserService}
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler}
import tsec.mac.jca.HMACSHA256
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds


object Server {
  def run[F[_] : Effect: ConcurrentEffect: Timer: ContextShift](httpConfig: HttpConfig,
                                                                auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[HMACSHA256, UserId]],
                                                                queries: Queries[F])(implicit ec: ExecutionContext) = {
    val userService = new UserService[F](auth, queries)
    val loginService = new LoginService[F](auth, queries)
    val statics = new StaticsService[F]()

    BlazeServerBuilder[F]
      .withHttpApp((statics.service <+> loginService.service <+> userService.service).orNotFound)
      .bindHttp(httpConfig.port, httpConfig.host)
      .serve.compile.drain.as(ExitCode.Success)
  }
}