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

package models.event.move

import models.Move
import no.uio.musit.time.dateTimeNow
import models.event.EventTypeRegistry.TopLevelEvents.{MoveNodeType, MoveObjectType}
import models.event.{EventType, MusitEvent}
import no.uio.musit.models.{ActorId, EventId, ObjectId, StorageNodeDatabaseId}
import org.joda.time.DateTime

sealed trait MoveEvent extends MusitEvent {
  val from: Option[StorageNodeDatabaseId]
  val to: StorageNodeDatabaseId
}

case class MoveObject(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[ObjectId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    from: Option[StorageNodeDatabaseId],
    to: StorageNodeDatabaseId
) extends MoveEvent

object MoveObject {

  def fromCommand(currUserId: ActorId, cmd: Move[ObjectId]): Seq[MoveObject] = {
    cmd.items.map { objectId =>
      val now = dateTimeNow
      MoveObject(
        id = None,
        doneBy = Option(currUserId),
        doneDate = now,
        affectedThing = Option(objectId),
        registeredBy = Option(currUserId),
        registeredDate = Option(now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        from = None,
        to = cmd.destination
      )
    }
  }
}

case class MoveNode(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[StorageNodeDatabaseId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    from: Option[StorageNodeDatabaseId],
    to: StorageNodeDatabaseId
) extends MoveEvent

object MoveNode {

  def fromCommand(
      currUserId: ActorId,
      cmd: Move[StorageNodeDatabaseId]
  ): Seq[MoveNode] = cmd.items.map { nodeId =>
    val now = dateTimeNow
    MoveNode(
      id = None,
      doneBy = Option(currUserId),
      doneDate = now,
      affectedThing = Option(nodeId),
      registeredBy = Option(currUserId),
      registeredDate = Option(now),
      eventType = EventType.fromEventTypeId(MoveNodeType.id),
      from = None,
      to = cmd.destination
    )
  }
}
