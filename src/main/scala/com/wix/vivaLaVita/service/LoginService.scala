package com.wix.vivaLaVita.service

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.monoid._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import com.wix.vivaLaVita.domain._
import com.wix.vivaLaVita.dto.UserDTO._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import com.wix.vivaLaVita.auth.LoginFlowCheck

class LoginService[F[_] : Sync](Auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[HMACSHA256, UserId]], loginCheck: LoginFlowCheck[F]) {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]

  import dsl._
  import serviceDsl._

  private def responseUser(user: User) = Ok(buildResponse(user).asJson)

  val service: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@POST -> Root / "login" =>
      req.as[UserLoginFlow] flatMap { uLogin =>
        (for {
          usr <- loginCheck(uLogin)
          resp <- OptionT.liftF(responseUser(usr))
          auth <- OptionT.liftF(Auth.authenticator.create(usr.id))
          authResponse <- OptionT.some(Auth.authenticator.embed(resp, auth))
        } yield authResponse).getOrElseF(forbidden)
      }
  }
}