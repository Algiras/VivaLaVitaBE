package com.wix.vivaLaVita.service

import java.util.UUID

import cats.Monad
import cats.data.OptionT
import cats.effect.{Effect, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
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

import scala.util.Try

class UserService[F[_] : Sync](Auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[HMACSHA256, UserId]], queries: Queries[F]) {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]

  import dsl._
  import serviceDsl._

  object UserIdVal {
    def unapply(arg: String): Option[UserId] = Try(UUID.fromString(arg)).toOption.map(tagUUIDAsUserId)
  }

  private def responseUser(user: User) = Ok(buildResponse(user).asJson)

  object PageQueryParamMatcher extends QueryParamDecoderMatcher[Int]("page")
  object PageSizeQueryParamMatcher extends QueryParamDecoderMatcher[Int]("page")

  val service: HttpRoutes[F] = Auth.liftService(TSecAuthService {
    case GET -> Root / "me" asAuthed user => responseUser(user)

    case GET -> Root / "users" :? PageQueryParamMatcher(page) +& PageSizeQueryParamMatcher(pageSize)  asAuthed _ =>
      queries.userDao.paged(page, pageSize).flatMap(res => Ok(res.map(buildResponse).asJson))

    case GET -> Root / "users" / UserIdVal(id)  asAuthed _ =>
      queries.userDao.read(id).flatMap(res => res.map(responseUser).getOrElse(notFound))

    case DELETE -> Root / "users" / UserIdVal(id)  asAuthed _ =>
      queries.userDao.read(id).flatMap(res => {
        res.map(_ => queries.userDao.delete(id)).map(_ => success).getOrElse(notFound)
      })

    case req@POST -> Root / "users" asAuthed _ =>
      req.request.as[UserRequest] flatMap (userReq => queries.userDao.create(buildUser(userReq)).map(responseUser).flatten)

    case req@PUT -> Root / "users" / UserIdVal(id)  asAuthed _ =>
      req.request.as[UserRequest] flatMap (user => queries.userDao.read(id).flatMap(userUpdate => {
        userUpdate
          .map(buildUpdatedUser(_, user))
          .map(u => queries.userDao.update(id, u).flatMap(_ => responseUser(u)))
          .getOrElse(notFound)
      }))
  })
}
