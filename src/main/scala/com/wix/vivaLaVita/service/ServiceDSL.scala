package com.wix.vivaLaVita.service

import cats.Monad
import cats.syntax.monoid._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import io.circe.Json
import org.http4s.Response
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag

class ServiceDSL[F[_]: Monad] {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  import dsl._

  private def buildMessage(message: String) = Json.obj(("message", Json.fromString(message)))

  val notFound: F[Response[F]] = NotFound(buildMessage("Not Found"))
  val forbidden: F[Response[F]] = Forbidden(buildMessage("Forbidden"))
  val success: F[Response[F]] = Ok(buildMessage("Success"))
}

object ServiceDSL {
  def apply[F[_]: Monad]: ServiceDSL[F] = new ServiceDSL[F]()
}
