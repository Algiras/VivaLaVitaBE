package com.wix.vivaLaVita.service

import java.util.UUID

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.monoid._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import com.wix.vivaLaVita.database.Queries
import com.wix.vivaLaVita.domain._
import com.wix.vivaLaVita.dto.UserDTO._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import cats.syntax.flatMap._

import scala.util.Try

class UserService[F[_] : Sync](queries: Queries[F]) {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]

  import dsl._
  import serviceDsl._

  object UserIdVal {
    def unapply(arg: String): Option[UserId] = Try(UUID.fromString(arg)).toOption.map(tagUUIDAsUserId)
  }

  private def responseUser(user: User) = Ok(buildResponse(user).asJson)

  object PageQueryParamMatcher extends QueryParamDecoderMatcher[Int]("page")
  object PageSizeQueryParamMatcher extends QueryParamDecoderMatcher[Int]("pageSize")

  val service: TSecAuthService[User, AugmentedJWT[HMACSHA256, UserId], F] = TSecAuthService {
    case GET -> Root / "me" asAuthed user => responseUser(user)

    case GET -> Root / "user" :? PageQueryParamMatcher(page) +& PageSizeQueryParamMatcher(pageSize)  asAuthed _ => {
      queries.userDao.paged(page, pageSize).flatMap(res => Ok(res.map(buildResponse).asJson))
    }

    case GET -> Root / "user" / UserIdVal(id)  asAuthed _ =>
      queries.userDao.read(id).flatMap(res => res.map(responseUser).getOrElse(notFound))

    case DELETE -> Root / "user" / UserIdVal(id)  asAuthed _ =>
      queries.userDao.read(id).flatMap(res => {
        res.map(_ => queries.userDao.delete(id)).map(_ => success).getOrElse(notFound)
      })

    case req@POST -> Root / "user" asAuthed _ => for {
      userReq <- req.request.as[UserRequest]
      passWord <- if(userReq.password.isDefined) Sync[F].delay(userReq.password.get) else Sync[F].raiseError[String](new Exception("Password is mandatory"))
      hashedPsw <-BCrypt.hashpw[F](passWord)
      res <- queries.userDao.create(buildUser(userReq.copy(password = Some(hashedPsw)))).map(responseUser).flatten
    } yield res

    case req@PUT -> Root / "user" / UserIdVal(id)  asAuthed _ => for {
      userReq <- req.request.as[UserRequest]
      passWord <- if(userReq.password.isDefined) Sync[F].delay(userReq.password.get) else Sync[F].raiseError[String](new Exception("Password is mandatory"))
      hashedPsw <-BCrypt.hashpw[F](passWord)
      res <- queries.userDao.read(id).flatMap(userUpdate => {
        userUpdate
          .map(buildUpdatedUser(_, userReq.copy(password = Some(hashedPsw))))
          .map(u => queries.userDao.update(id, u).flatMap(_ => responseUser(u)))
          .getOrElse(notFound)
      })
    } yield res
  }
}
