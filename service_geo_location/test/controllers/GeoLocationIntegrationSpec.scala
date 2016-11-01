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

import no.uio.musit.security.{BearerToken, FakeAuthenticator}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json.JsArray

import scala.language.postfixOps

class GeoLocationIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val queryParam = (adr: String) => s"/v1/address?search=$adr"

  val fakeToken = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "musitTestUser")

  "Using the GeoLocation API" when {
    "searching for addresses" should {
      "return a list of results matching the query paramter" in {
        val res = wsUrl(queryParam("Paal Bergsvei 56, Rykkinn"))
          .withHeaders(fakeToken.asHeader)
          .get().futureValue

        res.status mustBe Status.OK

        val jsArr = res.json.as[JsArray].value
        jsArr must not be empty

        (jsArr.head \ "street").as[String] mustBe "Paal Bergs vei"
        (jsArr.head \ "streetNo").as[String] mustBe "56"
        (jsArr.head \ "place").as[String] mustBe "RYKKINN"
        (jsArr.head \ "zip").as[String] mustBe "1348"
      }
    }
  }

}