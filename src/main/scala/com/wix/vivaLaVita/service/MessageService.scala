package com.wix.vivaLaVita.service

import java.util.UUID

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.monoid._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import com.wix.vivaLaVita.database.Queries
import com.wix.vivaLaVita.domain._
import com.wix.vivaLaVita.dto.MessageDTO._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.tag
import tsec.authentication._
import tsec.mac.jca.HMACSHA256

import scala.util.Try

class MessageService[F[_] : Sync : Monad](queries: Queries[F]) {
  val dsl: Http4sDsl[F] = Http4sDsl[F]
  val serviceDsl: ServiceDSL[F] = ServiceDSL[F]

  import dsl._
  import serviceDsl._

  private def checkIfValidId[T, U](messageReq: MessageRequest, getId: MessageRequest => Option[T], query: T => F[Option[U]]) =
    if (getId(messageReq).isDefined) {
      OptionT(query(getId(messageReq).get)).map(_ => messageReq)
    } else {
      OptionT.pure[F](messageReq)
    }

  object MessageIdVal {
    def unapply(arg: String): Option[MessageId] = Try(UUID.fromString(arg)).toOption.map(tagUUIDAsMessageId)
  }

  private def responseMessage(Message: Message) = Ok(buildResponse(Message).asJson)

  val service: TSecAuthService[User, AugmentedJWT[HMACSHA256, UserId], F] = TSecAuthService {
    case GET -> Root / "message" :? PageQueryParamMatcher(page) +& PageSizeQueryParamMatcher(pageSize) asAuthed _ =>
      queries.messageDao.paged(page, pageSize).flatMap(res => Ok(res.map(buildResponse).asJson))

    case GET -> Root / "message" / MessageIdVal(id) asAuthed _ =>
      queries.messageDao.read(id).flatMap(res => res.map(responseMessage).getOrElse(notFound))

    case DELETE -> Root / "message" / MessageIdVal(id) asAuthed _ =>
      queries.messageDao.read(id).flatMap(res => {
        res.map(_ => queries.messageDao.delete(id)).map(_ => success).getOrElse(notFound)
      })

    case req@POST -> Root / "message" asAuthed _ => (for {
      messageReq <- OptionT.liftF(req.request.as[MessageRequest])
      _ <- checkIfValidId(messageReq, _.positionId, queries.positionDao.read)
      res <- OptionT.liftF(queries.messageDao.create(buildMessage(messageReq)).map(responseMessage).flatten)
    } yield res).getOrElseF(badRequest)

    case req@PUT -> Root / "message" / MessageIdVal(id) asAuthed _ => (for {
      message <- OptionT.liftF(req.request.as[MessageRequest])
      _ <- checkIfValidId(message, _.positionId, queries.positionDao.read)
      res <- OptionT.liftF(queries.messageDao.read(id).flatMap(messageUpdate =>
        messageUpdate
          .map(buildUpdatedMessage(_, message))
          .map(u => queries.messageDao.update(id, u).flatMap(_ => responseMessage(u)))
          .getOrElse(notFound)))
    } yield res).getOrElseF(badRequest)
  }
}
