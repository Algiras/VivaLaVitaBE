package com.wix.vivaLaVita.service

import cats.effect.{ContextShift, Sync}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response, StaticFile}

import scala.concurrent.ExecutionContext.global

class StaticsService[F[_]: Sync] {
  val dsl = Http4sDsl[F]

  import dsl._

  private val statics = "/statics"

  def fetchResource(path: String, req: Request[F])(implicit cs: ContextShift[F]): F[Response[F]] = {
    StaticFile.fromResource(path, global, Some(req)).getOrElseF(NotFound())
  }

  def service(implicit cs: ContextShift[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root => fetchResource(statics + "/index.html", req)
  }
}