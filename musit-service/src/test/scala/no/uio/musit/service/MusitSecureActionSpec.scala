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

package no.uio.musit.service

import akka.stream.Materializer
import no.uio.musit.models.MuseumId
import no.uio.musit.models.Museums._
import no.uio.musit.security.fake.FakeAuthenticator.fakeAccessTokenPrefix
import no.uio.musit.security.Permissions._
import no.uio.musit.security._
import no.uio.musit.security.fake.FakeAuthenticator
import no.uio.musit.test.{FakeUsers, MusitSpecWithAppPerSuite}
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class MusitSecureActionSpec extends MusitSpecWithAppPerSuite {

  implicit lazy val materializer: Materializer = app.materializer

  class Dummy extends MusitActions {
    override val authService: Authenticator = new FakeAuthenticator
  }

  "A MusitSecureAction" when {

    "used without permissions on a controller" should {

      "return HTTP Unauthorized if bearer token is missing" in {
        val action = new Dummy().MusitSecureAction()(request => Ok)

        val req = FakeRequest(GET, "/")
        val res = call(action, req)

        status(res) mustEqual UNAUTHORIZED
      }

      "return OK if the request has a valid bearer token" in {
        val userId = FakeUsers.testReadId
        val token  = BearerToken(FakeUsers.testReadToken)

        val action = new Dummy().MusitSecureAction().async { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Future.successful(Ok(request.user.userInfo.id.asString))
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "return BAD_REQUEST if the museumId isn't valid" in {
        val userId = FakeUsers.testReadId
        val token  = BearerToken(FakeUsers.testReadToken)

        val action = new Dummy().MusitSecureAction(MuseumId(10)) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual BAD_REQUEST
      }
    }

    "used with Read restrictions on a controller" should {

      "accept requests if the user has read access to the museum" in {
        val userId = FakeUsers.testReadId
        val token  = BearerToken(FakeUsers.testReadToken)

        val action = new Dummy().MusitSecureAction(Test.id, Read) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "accept requests if the user has write access to the museum" in {
        val userId = FakeUsers.testWriteId
        val token  = BearerToken(FakeUsers.testWriteToken)

        val action = new Dummy().MusitSecureAction(Test.id, Read) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "accept requests if the user has God permission" in {
        val userId = FakeUsers.superUserId
        val token  = BearerToken(FakeUsers.superUserToken)

        val action = new Dummy().MusitSecureAction(Test.id, Read) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "reject requests if the user hasn't got read access to the museum" in {
        val userId = FakeUsers.nhmReadId
        val token  = BearerToken(FakeUsers.nhmReadToken)

        val action = new Dummy().MusitSecureAction(Test.id, Read)(_ => Ok)

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual FORBIDDEN
      }
    }

    "used with Write restrictions on a controller" should {
      "accept requests if the user has got write access to the museum" in {
        val userId = FakeUsers.nhmWriteId
        val token  = BearerToken(FakeUsers.nhmWriteToken)

        val action = new Dummy().MusitSecureAction(Nhm.id, Write) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "accept requests if the user has god permissions" in {
        val userId = FakeUsers.superUserId
        val token  = BearerToken(FakeUsers.superUserToken)

        val action = new Dummy().MusitSecureAction(Nhm.id, Write) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "reject requests if the user hasn't got write access to the museum" in {
        val userId = FakeUsers.nhmReadId
        val token  = BearerToken(FakeUsers.nhmReadToken)

        val action = new Dummy().MusitSecureAction(Nhm.id, Write)(_ => Ok)

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual FORBIDDEN
      }

    }

    "used with Admin restrictions on a controller" should {
      "accept requests if the user has got admin access to the museum" in {
        val userId = FakeUsers.nhmAdminId
        val token  = BearerToken(FakeUsers.nhmAdminToken)

        val action = new Dummy().MusitSecureAction(Nhm.id, Admin) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }
    }

    "used with God rights on a controller" should {
      "accept requests if the user has got God access to the application" in {
        val userId = FakeUsers.superUserId
        val token  = BearerToken(FakeUsers.superUserToken)

        val action = new Dummy().MusitSecureAction(Nhm.id, GodMode) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "not accept requests from un-Godly users" in {
        val userId = FakeUsers.nhmReadId
        val token  = BearerToken(FakeUsers.nhmReadToken)

        val action = new Dummy().MusitSecureAction(Nhm.id, GodMode)(_ => Ok)

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual FORBIDDEN
      }
    }
  }
}
