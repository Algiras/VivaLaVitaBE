package com.wix.vivaLaVita.dto

import java.util.UUID

import cats.effect.Sync
import com.wix.vivaLaVita.domain._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime
import shapeless.tag

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

  case class UserRequest(name: String, email: String, password: String)
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

  case class UserLogin(name: String, password: String)
  implicit val userLoginDecoder: Decoder[UserLogin] = deriveDecoder
  implicit def userLoginEntityDecoder[F[_]: Sync]: EntityDecoder[F, UserLogin] =
    jsonOf[F, UserLogin]
}