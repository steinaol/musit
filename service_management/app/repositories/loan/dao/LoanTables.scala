package repositories.loan.dao

import models.loan.LoanEventTypes.{ObjectLentType, ObjectReturnedType}
import models.loan.LoanType
import models.loan.event.{LoanEvent, ObjectsLent, ObjectsReturned}
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

/**
 * Defines the table definitions needed to work with analysis data.
 * Also includes a few helpful functions to convert between rows and types.
 */
trait LoanTables extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import profile.api._

  val loanTable       = TableQuery[LoanEventTable]
  val activeLoanTable = TableQuery[ActiveLoanTable]
  val lentObjectTable = TableQuery[LentObjectTable]

  // scalastyle:off line.size.limit
  type LoanEventRow = (
      Option[EventId],
      LoanType,
      Option[DateTime],
      Option[ActorId],
      Option[DateTime],
      MuseumId,
      Option[EventId],
      Option[ObjectUUID],
      Option[CaseNumbers],
      Option[String],
      JsValue
  )
  type ActiveLoanRow = (Option[Long], MuseumId, ObjectUUID, EventId, DateTime)
  type LentObjectRow = (Option[Long], EventId, ObjectUUID)
  // scalastyle:on line.size.limit

  class LoanEventTable(
      val tag: Tag
  ) extends Table[LoanEventRow](tag, Some(SchemaName), LoanEventTableName) {

    val id             = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val typeId         = column[LoanType]("TYPE_ID")
    val eventDate      = column[Option[DateTime]]("EVENT_DATE")
    val registeredBy   = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[DateTime]]("REGISTERED_DATE")
    val museumId       = column[MuseumId]("MUSEUM_ID")
    val partOf         = column[Option[EventId]]("PART_OF")
    val objectUuid     = column[Option[ObjectUUID]]("OBJECT_UUID")
    val caseNumbers    = column[Option[CaseNumbers]]("CASE_NUMBERS")
    val note           = column[Option[String]]("NOTE")
    val eventJson      = column[JsValue]("EVENT_JSON")

    // scalastyle:off method.name line.size.limit
    override def * =
      (
        id.?,
        typeId,
        eventDate,
        registeredBy,
        registeredDate,
        museumId,
        partOf,
        objectUuid,
        caseNumbers,
        note,
        eventJson
      )

    // scalastyle:on method.name line.size.limit
  }

  class ActiveLoanTable(
      val tag: Tag
  ) extends Table[ActiveLoanRow](tag, Some(SchemaName), ActiveLoanTableName) {

    val id         = column[Long]("ACTIVE_LOAN_ID", O.PrimaryKey, O.AutoInc)
    val museumId   = column[MuseumId]("MUSEUM_ID")
    val objectUuid = column[ObjectUUID]("OBJECT_UUID")
    val eventId    = column[EventId]("EVENT_ID")
    val returnDate = column[DateTime]("RETURN_DATE")

    // scalastyle:off method.name line.size.limit
    override def * = (id.?, museumId, objectUuid, eventId, returnDate)

    // scalastyle:on method.name line.size.limit
  }

  class LentObjectTable(
      val tag: Tag
  ) extends Table[LentObjectRow](tag, Some(SchemaName), LentObjectTableName) {

    val id         = column[Long]("LENT_OBJECT_ID", O.PrimaryKey, O.AutoInc)
    val eventId    = column[EventId]("EVENT_ID")
    val objectUuid = column[ObjectUUID]("OBJECT_UUID")

    // scalastyle:off method.name line.size.limit
    override def * = (id.?, eventId, objectUuid)

    // scalastyle:on method.name line.size.limit
  }

  private[dao] def asEventRowTuple(mid: MuseumId, ol: ObjectsLent): LoanEventRow =
    (
      ol.id,
      ol.loanType,
      ol.eventDate,
      ol.registeredBy,
      ol.registeredDate,
      mid,
      ol.partOf,
      ol.objectId,
      ol.caseNumbers,
      ol.note,
      Json.toJson[ObjectsLent](ol)
    )

  private[dao] def asEventRowTuple(
      mid: MuseumId,
      returnedObject: ObjectsReturned
  ): LoanEventRow = (
    returnedObject.id,
    returnedObject.loanType,
    returnedObject.eventDate,
    returnedObject.registeredBy,
    returnedObject.registeredDate,
    mid,
    returnedObject.partOf,
    returnedObject.objectId,
    returnedObject.caseNumbers,
    returnedObject.note,
    Json.toJson[ObjectsReturned](returnedObject)
  )

  private[dao] def fromLoanEventRow(tuple: LoanEventRow): LoanEvent = {
    tuple._2 match {
      case ObjectLentType =>
        tuple._11.as[ObjectsLent]

      case ObjectReturnedType =>
        tuple._11.as[ObjectsReturned]
    }
  }
}
