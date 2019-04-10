package com.wix.vivaLaVita

import shapeless.tag
import java.util.UUID
import com.wix.vivaLaVita.database.dao.Identifiable
import org.joda.time.DateTime
import shapeless.tag.@@

package object domain {

  trait UserIdTag

  type UserId = UUID @@ UserIdTag

  def tagUUIDAsUserId(id: UUID): UserId = tag[UserIdTag][UUID](id)

  case class User(id: UserId,
                  name: String,
                  email: String,
                  password: String,
                  createdAt: DateTime,
                  updatedAt: Option[DateTime],
                  isActive: Boolean) extends Identifiable[UserId, User] {
    override def getId(value: User): UserId = id
  }
}
