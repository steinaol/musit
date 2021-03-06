package repositories.storage.dao

import no.uio.musit.models._
import play.api.db.slick.HasDatabaseConfigProvider
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

private[dao] trait SharedTables
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  val localObjectsTable = TableQuery[LocalObjectsTable]

  type LocalObjectRow = (ObjectUUID, EventId, StorageNodeId, MuseumId, Option[String])

  class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocalObjectRow](tag, SchemaName, "NEW_LOCAL_OBJECT") {
    // scalastyle:off method.name
    def * =
      (
        objectUuid,
        latestMoveId,
        currentLocationId,
        museumId,
        objectType
      )

    // scalastyle:on method.name

    val objectUuid        = column[ObjectUUID]("OBJECT_UUID", O.PrimaryKey)
    val latestMoveId      = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeId]("CURRENT_LOCATION_ID")
    val museumId          = column[MuseumId]("MUSEUM_ID")
    val objectType        = column[Option[String]]("OBJECT_TYPE")

  }

}
