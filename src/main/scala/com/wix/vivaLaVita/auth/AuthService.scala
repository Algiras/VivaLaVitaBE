package com.wix.vivaLaVita.auth

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.functor._
import com.wix.vivaLaVita.database.dao.UserDAO
import com.wix.vivaLaVita.domain._
import com.wix.vivaLaVita.dto._
import tsec.authentication.{AugmentedJWT, BackingStore, JWTAuthenticator, SecuredRequestHandler, TSecAuthService}
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.concurrent.duration._

object AuthService {
  def apply[F[_] : Sync](key: MacSigningKey[HMACSHA256], userDao: UserDAO[F]): SecuredRequestHandler[F, UserId, User, AugmentedJWT[HMACSHA256, UserId]] = {
    type AuthService = TSecAuthService[User, AugmentedJWT[HMACSHA256, Int], F]

    val userStore: BackingStore[F, UserId, User] = new BackingStore[F, UserId, User] {
      override def put(user: User): F[User] = userDao.create(user)
      override def update(user: User): F[User] = userDao.update(user.id, user).map(_ => user)
      override def delete(id: UserId): F[Unit] = userDao.delete(id)
      override def get(id: UserId): OptionT[F, User] = OptionT(userDao.read(id))
    }

    val jwtStatefulAuth: JWTAuthenticator[F, UserId, User, HMACSHA256] = JWTAuthenticator.unbacked.inBearerToken(
      expiryDuration = 10.minutes,
      maxIdle = None,
      signingKey = key,
      identityStore = userStore
    )
    SecuredRequestHandler(jwtStatefulAuth)
  }
}
