package com.wix.vivaLaVita.auth

import java.util.Collections

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
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

  val GOOGLE_API_KEY = s"407408718192.apps.googleusercontent.com"

  val verifier: GoogleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
    .setAudience(Collections.singletonList(GOOGLE_API_KEY))
    .build()

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

  def googleFlow(token : String) = {
    OptionT.liftF(for {
      verifiedToken <- Sync[F].delay(verifier.verify(token))
      payload <- Sync[F].delay(verifiedToken.getPayload)
      email <- Sync[F].delay(payload.get("email").asInstanceOf[String])
      name <- Sync[F].delay(payload.get("name").asInstanceOf[String])
      userOpt <- userDao.select(email)
      user <- if(userOpt.isDefined) Monad[F].pure(userOpt.get) else userDao.create(UserDTO.buildUser(UserRequest(name, email, None)))
    } yield user)
  }
}
