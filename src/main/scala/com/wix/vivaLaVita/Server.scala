package com.wix.vivaLaVita

import cats.effect._
import cats.implicits._
import com.wix.vivaLaVita.config.HttpConfig
import com.wix.vivaLaVita.database.Queries
import com.wix.vivaLaVita.domain.{User, UserId}
import com.wix.vivaLaVita.service._
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
    val userService = new UserService[F](queries)
    val loginService = new LoginService[F](auth, queries)
    val statics = new StaticsService[F]()
    val typeService = new TypesService[F]()
    val positionService = new PositionService[F](queries)
    val messageService = new MessageService[F](queries)

    BlazeServerBuilder[F]
      .withHttpApp((statics.service <+> loginService.service
        <+> auth.liftService(
              typeService.service <+>
              userService.service <+>
              positionService.service <+> messageService.service
        )
      ).orNotFound)
      .bindHttp(httpConfig.port, httpConfig.host)
      .serve.compile.drain.as(ExitCode.Success)
  }
}