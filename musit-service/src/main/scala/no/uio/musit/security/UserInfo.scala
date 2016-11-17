/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.security

import no.uio.musit.models.ActorId
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UserInfo(
  id: ActorId,
  name: Option[String],
  email: Option[String],
  picture: Option[String]
)

object UserInfo {

  implicit val format: Format[UserInfo] = (
    (__ \ "userid").format[ActorId] and
    (__ \ "name").formatNullable[String] and
    (__ \ "email").formatNullable[String] and
    (__ \ "profilephoto").formatNullable[String]
  )(UserInfo.apply, unlift(UserInfo.unapply))

}
