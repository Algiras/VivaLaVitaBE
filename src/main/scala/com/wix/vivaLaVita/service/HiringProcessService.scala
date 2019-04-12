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
import com.wix.vivaLaVita.dto.HiringProcessDTO._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag
import tsec.authentication._
import tsec.mac.jca.HMACSHA256

import scala.util.Try

class HiringProcessService[F[_] : Sync](queries: Queries[F]) {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]

  import dsl._
  import serviceDsl._

  object HiringProcessIdVal {
    def unapply(arg: String): Option[HiringProcessId] = Try(UUID.fromString(arg)).toOption.map(tagUUIDAsHiringProcessId)
  }

  private def responseHiringProcess(hiringProcess: HiringProcess) = Ok(buildResponse(hiringProcess).asJson)

  val service: TSecAuthService[User, AugmentedJWT[HMACSHA256, UserId], F] = TSecAuthService {
    case GET -> Root / "hiringProcess" :? PageQueryParamMatcher(page) +& PageSizeQueryParamMatcher(pageSize) asAuthed _ =>
      queries.hiringProcessDao.paged(page, pageSize).flatMap(res => Ok(res.map(buildResponse).asJson))

    case GET -> Root / "hiringProcess" / HiringProcessIdVal(id) asAuthed _ =>
      queries.hiringProcessDao.read(id).flatMap(res => res.map(responseHiringProcess).getOrElse(notFound))

    case DELETE -> Root / "hiringProcess" / HiringProcessIdVal(id) asAuthed _ =>
      queries.hiringProcessDao.read(id).flatMap(res => {
        res.map(_ => queries.hiringProcessDao.delete(id)).map(_ => success).getOrElse(notFound)
      })

    case req@POST -> Root / "hiringProcess" asAuthed _ => {
      for {
        hiringProcessRequest <- req.request.as[HiringProcessRequest]
        _ <- guardValueNotDefined("positionId")(queries.positionDao.read(hiringProcessRequest.positionId))
        _ <- guardValueNotDefined("candidateId")(queries.candidateDao.read(hiringProcessRequest.candidateId))
        res <- queries.hiringProcessDao.create(buildHiringProcess(hiringProcessRequest)).map(responseHiringProcess).flatten
      } yield res
    }

    case req@PUT -> Root / "hiringProcess" / HiringProcessIdVal(id) asAuthed _ => for {
      hiringProcessUpdate <- req.request.as[HiringProcessUpdateRequest]
      res <- queries.hiringProcessDao.read(id).flatMap(hiringProcess => {
        hiringProcess
          .map(buildUpdatedHiringProcess(_, hiringProcessUpdate))
          .map(u => queries.hiringProcessDao.update(id, u).flatMap(_ => responseHiringProcess(u)))
          .getOrElse(notFound)
      })
    } yield res
  }
}
