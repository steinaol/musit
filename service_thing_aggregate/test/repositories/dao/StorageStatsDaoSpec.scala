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

package repositories.dao

import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models._
import no.uio.musit.test.MusitSpecWithAppPerSuite

/**
 * ¡¡¡This spec relies on objects being inserted in the evolution script under
 * {{{src/test/resources/evolutions/default/1.sql script.}}}.
 * This is achieved by using relaxed constraints on primary and foreign key
 * references in comparison to the proper schema!!!
 */
class StorageStatsDaoSpec extends MusitSpecWithAppPerSuite {

  val statsDao = fromInstanceCache[StorageStatsDao]

  "StorageStatsDao" should {

    "return the number of direct child nodes" in {
      val nodeId = StorageNodeDatabaseId(4)
      statsDao.numChildren(nodeId).futureValue mustBe MusitSuccess(3)
    }

    "return the number of objects on a node" in {
      val nodeId = StorageNodeDatabaseId(6)
      statsDao.numObjectsInNode(nodeId).futureValue mustBe MusitSuccess(34)
    }

    "return the total number of objects i a node hierarchy" in {
      val path = NodePath(",1,")
      statsDao.numObjectsInPath(path).futureValue mustBe MusitSuccess(52)
    }

  }

}
