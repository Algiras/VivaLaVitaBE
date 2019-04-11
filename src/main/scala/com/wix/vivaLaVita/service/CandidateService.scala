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
import com.wix.vivaLaVita.dto.CandidateDTO._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag
import tsec.authentication._
import tsec.mac.jca.HMACSHA256

import scala.util.Try

class CandidateService[F[_] : Sync](queries: Queries[F]) {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]

  import dsl._
  import serviceDsl._

  object CandidateIdVal {
    def unapply(arg: String): Option[CandidateId] = Try(UUID.fromString(arg)).toOption.map(tagUUIDAsCandidateId)
  }

  private def responseCandidate(Candidate: Candidate) = Ok(buildResponse(Candidate).asJson)

  val service: TSecAuthService[User, AugmentedJWT[HMACSHA256, UserId], F] = TSecAuthService {
    // get Candidate by Name ~> return his info and hiringProcessInfo

    case GET -> Root / "candidate" :? PageQueryParamMatcher(page) +& PageSizeQueryParamMatcher(pageSize)  asAuthed _ =>
      queries.candidateDao.paged(page, pageSize).flatMap(res => Ok(res.map(buildResponse).asJson))

    case GET -> Root / "candidate" / CandidateIdVal(id)  asAuthed _ =>
      queries.candidateDao.read(id).flatMap(res => res.map(responseCandidate).getOrElse(notFound))

    case DELETE -> Root / "candidate" / CandidateIdVal(id)  asAuthed _ =>
      queries.candidateDao.read(id).flatMap(res => {
        res.map(_ => queries.candidateDao.delete(id)).map(_ => success).getOrElse(notFound)
      })

    case req@POST -> Root / "candidate" asAuthed _ =>
      req.request.as[CandidateRequest] flatMap (CandidateRequest => queries.candidateDao.create(buildCandidate(CandidateRequest)).map(responseCandidate).flatten)

    case req@PUT -> Root / "candidate" / CandidateIdVal(id)  asAuthed _ =>
      req.request.as[CandidateRequest] flatMap (Candidate => queries.candidateDao.read(id).flatMap(CandidateUpdate => {
        CandidateUpdate
          .map(buildUpdatedCandidate(_, Candidate))
          .map(u => queries.candidateDao.update(id, u).flatMap(_ => responseCandidate(u)))
          .getOrElse(notFound)
      }))
  }
}
