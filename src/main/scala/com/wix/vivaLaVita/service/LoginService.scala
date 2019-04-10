package com.wix.vivaLaVita.service

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.monoid._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import com.wix.vivaLaVita.database.Queries
import com.wix.vivaLaVita.domain._
import com.wix.vivaLaVita.dto.UserDTO._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers._
import tsec.passwordhashers.jca._
import cats.syntax.functor._

class LoginService[F[_] : Sync](Auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[HMACSHA256, UserId]], queries: Queries[F]) {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]

  import dsl._
  import serviceDsl._

  private def responseUser(user: User) = Ok(buildResponse(user).asJson)

  val service: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@POST -> Root / "login" =>
      req.as[UserLogin] flatMap { uLogin =>
        (for {
          usr <- OptionT(queries.userDao.select(uLogin.name))
          isValid <- OptionT.liftF(BCrypt.checkpwBool[F](uLogin.password, PasswordHash[BCrypt](usr.password))) if isValid
          resp <- OptionT.liftF(responseUser(usr))
          auth <- OptionT.liftF(Auth.authenticator.create(usr.id))
          authResponse <- OptionT.some(Auth.authenticator.embed(resp, auth))
        } yield authResponse).getOrElseF(forbidden)
      }
  }
}