package com.wix.vivaLaVita.service

import cats.effect.Sync
import cats.syntax.monoid._
import cats.syntax.flatMap._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import io.circe.Json
import org.http4s.Response
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag

class ServiceDSL[F[_]: Sync] {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  import dsl._

  object PageQueryParamMatcher extends QueryParamDecoderMatcher[Int]("page")
  object PageSizeQueryParamMatcher extends QueryParamDecoderMatcher[Int]("pageSize")

  object OptionalFullNameParamMatcher extends OptionalQueryParamDecoderMatcher[String]("fullName")
  object OptionalLinkParamMatcher extends OptionalQueryParamDecoderMatcher[String]("link")
  object OptionalEmailParamMatcher extends OptionalQueryParamDecoderMatcher[String]("email")

  private def buildMessage(message: String) = Json.obj(("message", Json.fromString(message)))

  val notFound: F[Response[F]] = NotFound(buildMessage("Not Found"))
  val forbidden: F[Response[F]] = Forbidden(buildMessage("Forbidden"))
  val success: F[Response[F]] = Ok(buildMessage("Success"))
  val badRequest: F[Response[F]] = BadRequest(buildMessage("Bad Request"))

  def guardBuilder[A](msg: String => String)(name: String)(value: F[Option[A]]) = value.flatMap(v => if(v.isDefined) {
    Sync[F].pure(v.get)
  } else {
    Sync[F].raiseError[A](new Exception(msg(name)))
  })

  def guardValueNotDefined[A]: String => F[Option[A]] => F[A] = guardBuilder[A](name => s"$name is not defined")
  def guardValueNotInSystem[A]: String => F[Option[A]] => F[A] = guardBuilder[A](name => s"$name is not defined in the system")
}

object ServiceDSL {
  def apply[F[_]: Sync]: ServiceDSL[F] = new ServiceDSL[F]()
}
