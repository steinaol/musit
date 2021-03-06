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

package no.uio.musit.models

import java.util.{NoSuchElementException, UUID}

import scala.util.Try

trait MusitUUID {

  val underlying: UUID

  def asString: String = underlying.toString

}

trait MusitUUIDOps[T <: MusitUUID] {

  implicit def fromUUID(uuid: UUID): T

  lazy val empty = unsafeFromString("00000000-0000-0000-0000-000000000000")

  @throws(classOf[NoSuchElementException])
  def unsafeFromString(str: String): T = fromString(str).get

  def validate(str: String): Try[UUID] = Try(UUID.fromString(str))

  def generate(): T

  def generateAsOpt(): Option[T] = Option(generate())

  def fromString(str: String): Option[T] = validate(str).toOption.map(fromUUID)

}
