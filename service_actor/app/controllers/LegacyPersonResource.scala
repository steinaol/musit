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

package controllers

import com.google.inject.Inject
import models.Person
import no.uio.musit.security.Authenticator
import no.uio.musit.service.{MusitController, MusitSearch}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.LegacyPersonService

import scala.concurrent.Future

class LegacyPersonResource @Inject() (
    val authService: Authenticator,
    val service: LegacyPersonService
) extends MusitController {

  def search(
    museumId: Int,
    search: Option[MusitSearch]
  ) = MusitSecureAction().async { request =>
    search match {
      case Some(criteria) =>
        service.find(criteria).map(persons => Ok(Json.toJson(persons)))

      case None =>
        Future.successful(
          BadRequest(Json.obj("message" -> "Search criteria is required"))
        )
    }
  }

  def details = MusitSecureAction().async(parse.json) { request =>
    request.body.validate[Seq[Long]] match {
      case JsSuccess(ids, path) =>
        service.findDetails(ids.toSet).map { persons =>
          if (persons.isEmpty) {
            NoContent
          } else {
            Ok(Json.toJson(persons))
          }
        }

      case e: JsError =>
        Future.successful(BadRequest(Json.obj("message" -> e.toString)))
    }
  }

  def get(id: Long) = MusitSecureAction().async { request =>
    service.find(id).map {
      case Some(actor) =>
        Ok(Json.toJson(actor))

      case None =>
        NotFound(Json.obj("message" -> s"Did not find object with id: $id"))
    }
  }

  def add = MusitSecureAction().async(parse.json) { request =>
    request.body.validate[Person] match {
      case s: JsSuccess[Person] =>
        service.create(s.get).map(newActor => Created(Json.toJson(newActor)))

      case e: JsError =>
        Future.successful(BadRequest(Json.obj("message" -> e.toString)))
    }
  }
}