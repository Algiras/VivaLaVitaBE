package com.wix.vivaLaVita.auth

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.oauth2.Oauth2
import com.wix.vivaLaVita.database.dao.UserDAO
import com.wix.vivaLaVita.domain.AuthTokenType.Google
import com.wix.vivaLaVita.domain.User
import com.wix.vivaLaVita.dto.UserDTO
import com.wix.vivaLaVita.dto.UserDTO.{PasswordFlow, TokenFlow, UserLoginFlow, UserRequest}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt



case class LoginFlowCheck[F[_]: Sync](userDao: UserDAO[F], googleApiKey: String) {
  lazy val httpTransport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport
  lazy val jsonFactory: JacksonFactory = JacksonFactory.getDefaultInstance

  val GOOGLE_API_KEY = s"$googleApiKey.apps.googleusercontent.com"

  def guardCheck[A](expected: A, name: String)(value: A) = if(expected == value) {
    Sync[F].pure(())
  } else {
    Sync[F].raiseError[Unit](new Exception(s"$name does not match"))
  }

  def notNull[A](name: String)(value: A) = if(value == null) {
    Sync[F].raiseError[A](new Exception(s"$name can not be null!"))
  } else Sync[F].pure(value)

  def apply(flow: UserLoginFlow): OptionT[F, User] = flow match {
    case flow: PasswordFlow => passwordFlow(flow)
    case TokenFlow(Google, token) => googleFlow(token)
    case TokenFlow(_, _) => ???
  }

  def passwordFlow(flow: PasswordFlow): OptionT[F, User] = for {
    usr <- OptionT(userDao.select(flow.email))
    userPassword <- OptionT.fromOption(usr.password)
    isValid <- OptionT.liftF(BCrypt.checkpwBool[F](flow.password, PasswordHash[BCrypt](userPassword))) if isValid
  } yield usr

  private def buildUser(oauth2: Oauth2, email: String, token: String): F[User] = for {
    userInfo <- Sync[F].delay(oauth2.userinfo.get.execute)
    name <- Sync[F].delay(userInfo.get("name").asInstanceOf[String])
    user <- userDao.create(UserDTO.buildUser(UserRequest(name, email, None)))
  } yield user

  def googleFlow(token : String) = {
    val credential: GoogleCredential = new GoogleCredential().setAccessToken(token)
    val oauth2: Oauth2 = new Oauth2.Builder(httpTransport, jsonFactory, credential ).build()

    OptionT.liftF(for {
      tokenInfo <- Sync[F].delay(oauth2.tokeninfo.setAccessToken(token).execute)
      _ <- Sync[F].delay(tokenInfo.get("audience").asInstanceOf[String]).flatMap(guardCheck(GOOGLE_API_KEY, "Audience"))
      email <- Sync[F].delay(tokenInfo.get("email").asInstanceOf[String]).flatMap(notNull("email"))
      userOpt <- userDao.select(email)
      user <- if(userOpt.isDefined) Monad[F].pure(userOpt.get) else buildUser(oauth2, email, token)
    } yield user)
  }
}
