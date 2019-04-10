package com.wix.vivaLaVita.service

import cats.effect.Sync
import cats.syntax.monoid._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import com.wix.vivaLaVita.domain._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag
import tsec.authentication.{AugmentedJWT, TSecAuthService, asAuthed}
import tsec.mac.jca.HMACSHA256

class TypesService[F[_] : Sync]() {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]
  import dsl._

  val service: TSecAuthService[User, AugmentedJWT[HMACSHA256, UserId], F] = TSecAuthService {
    case GET -> Root / "link-type" asAuthed _ => Ok(LinkType.values.asJson)
    case GET -> Root / "candidate-type" asAuthed _ => Ok(CandidateType.values.asJson)
    case GET -> Root / "hiring-process-status" asAuthed _ => Ok(HiringProcessStatus.values.asJson)
  }
}
