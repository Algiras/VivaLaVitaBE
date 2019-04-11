package com.wix.vivaLaVita.dto

import java.util.UUID

import cats.effect.Sync
import com.wix.vivaLaVita.domain._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime
import shapeless.tag
import cats.syntax.functor._
import io.circe.{ Decoder, Encoder }
import io.circe.generic.auto._
import io.circe.syntax._

object UserDTO {

  case class UserResponse(id: UserId, name: String, email: String, createdAt: DateTime, updatedAt: Option[DateTime])
  implicit val userResponseEncoder: Encoder[UserResponse] = deriveEncoder

  def buildResponse(user: User): UserResponse = UserResponse(
    id = user.id,
    name = user.name,
    email = user.email,
    createdAt = user.createdAt,
    updatedAt = user.updatedAt
  )

  case class UserRequest(name: String, email: String, password: Option[String])
  implicit val userRequestDecoder: Decoder[UserRequest] = deriveDecoder
  implicit def userRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, UserRequest] =
    jsonOf[F, UserRequest]

  def buildUser(user: UserRequest): User = User(
    id = tagUUIDAsUserId(UUID.randomUUID()),
    name = user.name,
    email = user.email,
    password = user.password,
    createdAt = DateTime.now(),
    updatedAt = None,
    isActive = true
  )

  def buildUpdatedUser(user: User, userUpdate: UserRequest): User = {
    user.copy(
      email = userUpdate.email,
      password = userUpdate.password,
      updatedAt = Some(DateTime.now)
    )
  }

  implicit val LoginFlowEncoder: Encoder[AuthTokenType] = deriveEncoder

  sealed trait UserLoginFlow
  final case class PasswordFlow(email: String, password: String) extends UserLoginFlow
  final case class TokenFlow(`type`: AuthTokenType, token: String) extends UserLoginFlow

  implicit val passwordFlowDecoder: Decoder[PasswordFlow] = deriveDecoder
  implicit val tokenFlowDecoder: Decoder[TokenFlow] = deriveDecoder

  implicit def userLoginViaPasswordEntityDecoder[F[_]: Sync]: EntityDecoder[F, PasswordFlow] =
    jsonOf[F, PasswordFlow]
  implicit def userLoginViaTokenEntityDecoder[F[_]: Sync]: EntityDecoder[F, TokenFlow] =
    jsonOf[F, TokenFlow]

  implicit val userLoginFlowDecoder: Decoder[UserLoginFlow] = List[Decoder[UserLoginFlow]](
    Decoder[PasswordFlow].widen,
    Decoder[TokenFlow].widen
  ).reduceLeft(_ or _)

  implicit def userLoginEntityDecoder[F[_]: Sync]: EntityDecoder[F, UserLoginFlow] =
    jsonOf[F, UserLoginFlow]

}