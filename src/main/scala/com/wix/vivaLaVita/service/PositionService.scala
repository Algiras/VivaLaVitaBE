package com.wix.vivaLaVita.service

import java.util.UUID

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.monoid._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import com.wix.vivaLaVita.database.Queries
import com.wix.vivaLaVita.domain._
import com.wix.vivaLaVita.dto.PositionDTO._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag
import tsec.authentication._
import tsec.mac.jca.HMACSHA256

import scala.util.Try

class PositionService[F[_] : Sync](queries: Queries[F]) {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]

  import dsl._
  import serviceDsl._

  object PositionIdVal {
    def unapply(arg: String): Option[PositionId] = Try(UUID.fromString(arg)).toOption.map(tagUUIDAsPositionId)
  }

  private def responsePosition(position: Position) = Ok(buildResponse(position).asJson)

  val service: TSecAuthService[User, AugmentedJWT[HMACSHA256, UserId], F] = TSecAuthService {
    case GET -> Root / "position" :? PageQueryParamMatcher(page) +& PageSizeQueryParamMatcher(pageSize)  asAuthed _ =>
      queries.positionDao.paged(page, pageSize).flatMap(res => Ok(res.map(buildResponse).asJson))

    case GET -> Root / "position" / PositionIdVal(id)  asAuthed _ =>
      queries.positionDao.read(id).flatMap(res => res.map(responsePosition).getOrElse(notFound))

    case DELETE -> Root / "position" / PositionIdVal(id)  asAuthed _ =>
      queries.positionDao.read(id).flatMap(res => {
        res.map(_ => queries.positionDao.delete(id)).map(_ => success).getOrElse(notFound)
      })

    case req@POST -> Root / "position" asAuthed _ =>
      req.request.as[PositionRequest] flatMap (positionRequest => queries.positionDao.create(buildPosition(positionRequest)).map(responsePosition).flatten)

    case req@PUT -> Root / "position" / PositionIdVal(id)  asAuthed _ =>
      req.request.as[PositionRequest] flatMap (position => queries.positionDao.read(id).flatMap(positionUpdate => {
        positionUpdate
          .map(buildUpdatedPosition(_, position))
          .map(u => queries.positionDao.update(id, u).flatMap(_ => responsePosition(u)))
          .getOrElse(notFound)
      }))
  }
}
