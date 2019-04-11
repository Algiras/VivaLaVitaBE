package com.wix.vivaLaVita

import cats.effect._
import cats.implicits._
import cats.~>
import com.wix.vivaLaVita.auth.LoginFlowCheck
import com.wix.vivaLaVita.config.HttpConfig
import com.wix.vivaLaVita.database.Queries
import com.wix.vivaLaVita.domain.{User, UserId}
import com.wix.vivaLaVita.service._
import org.http4s.Http
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler}
import tsec.mac.jca.HMACSHA256
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds


object Server {

  def run[F[_] : Effect: ConcurrentEffect: Timer: ContextShift](httpConfig: HttpConfig,
                                                                loginCheck: LoginFlowCheck[F],
                                                                auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[HMACSHA256, UserId]],
                                                                queries: Queries[F])(implicit ec: ExecutionContext) = {
    val userService = new UserService[F](queries)
    val loginService = new LoginService[F](auth, loginCheck)
    val statics = new StaticsService[F]()
    val typeService = new TypesService[F]()
    val positionService = new PositionService[F](queries)
    val messageService = new MessageService[F](queries)
    val candidateService = new CandidateService[F](queries)


    val transform = new (F ~> F){
      override def apply[A](fa: F[A]): F[A] = fa
    }

    val logger: Http[F, F] => Http[F, F] = Logger(logHeaders = true, logBody = true, transform)

    BlazeServerBuilder[F]
      .withHttpApp(logger(CORS((statics.service <+> loginService.service
        <+> auth.liftService(
              typeService.service <+>
              userService.service <+>
              positionService.service <+>
              messageService.service <+>
              candidateService.
                service
        )
      ).orNotFound)))
      .bindHttp(httpConfig.port, httpConfig.host)
      .serve.compile.drain.as(ExitCode.Success)
  }
}