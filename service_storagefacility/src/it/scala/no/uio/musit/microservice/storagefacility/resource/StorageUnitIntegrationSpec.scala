package no.uio.musit.microservice.storagefacility.resource

import no.uio.musit.microservice.storagefacility.domain.storage.StorageType._
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.test.StorageNodeJsonGenerator._
import no.uio.musit.microservice.storagefacility.test._
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse


/**
 * TODO: These tests are still somewhat fragile in that some of them
 * depend on ID's generated by the previously executed test cases.
 */
class StorageUnitIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  def verifyNode[T <: StorageNode](
    response: WSResponse,
    expStorageType: StorageType,
    expName: String,
    expId: Long,
    expPartOf: Option[Long] = None
  )(implicit manifest: Manifest[T]): T = {
    val storageNode = parseAndVerifyResponse[T](response)
    // verifying common attributes across all storage node types
    storageNode.id mustBe Some(StorageNodeId(expId))
    storageNode.storageType mustBe expStorageType
    storageNode.isPartOf mustBe expPartOf.map(StorageNodeId.apply)
    storageNode.name mustBe expName
    storageNode mustBe a[T]

    storageNode
  }

  def parseAndVerifyResponse[T](response: WSResponse): T = {
    val json = Json.parse(response.body)
    val parsed = json.validate[StorageNode]
    parsed.isSuccess mustBe true
    parsed.get.asInstanceOf[T]
  }


  "Running the storage facility service" when {

    "interacting with the StorageUnitResource endpoints" should {

      "successfully create a few root nodes" in {

        def generateAndAddRootNode(numNodes: Int): Seq[Root] = {
          val addedNodes = Seq.newBuilder[Root]
          for (i <- 1 to numNodes) {
            val res = wsUrl(RootNodeUrl).post(JsNull).futureValue
            val root1 = parseAndVerifyResponse[Root](res)
            root1 mustBe a[Root]
            root1.id.isEmpty must not be true
            root1.storageType mustBe StorageType.RootType
            addedNodes += root1
          }
          addedNodes.result()
        }

        generateAndAddRootNode(4).size mustBe 4
      }

      "successfully create an organisation node" in {
        val json = organisationJson("My Org1")
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED

        val organisation = verifyNode[Organisation](
          response, OrganisationType, "My Org1", 5
        )
        organisation mustBe an[Organisation]
      }

      "successfully create a building node" in {
        val json = buildingJson("My Building1", StorageNodeId(5))
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED

        val building = verifyNode[Building](
          response, BuildingType, "My Building1", 6, Some(5)
        )
        building mustBe a[Building]
      }

      "successfully create a room node" in {
        val json = roomJson("My Room1", StorageNodeId(6))
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED

        val room = verifyNode[Room](
          response, RoomType, "My Room1", 7, Some(6)
        )
        room mustBe a[Room]
      }

      "successfully create a storage unit node" in {
        val json = storageUnitJson("My Shelf1", StorageNodeId(7))
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED

        val su = verifyNode[StorageUnit](
          response, StorageUnitType, "My Shelf1", 8, Some(7)
        )
        su mustBe a[StorageUnit]
      }

      "not allow creating a storage node with a name over 500 chars" in {
        val json = storageUnitJson(VeryLongString, StorageNodeId(3))
        val response = wsUrl(StorageNodesUrl).post(json).futureValue

        response.status mustBe Status.BAD_REQUEST
      }

      "not allow creating a building with an address over 500 chars" in {
        val bjs = buildingJson("fail", StorageNodeId(3))
        val json = bjs.as[JsObject] ++ Json.obj("address" -> VeryLongString)

        val response = wsUrl(StorageNodesUrl).post(json).futureValue

        response.status mustBe Status.BAD_REQUEST
      }

      "successfully get an organisation" in {
        val response = wsUrl(StorageNodeUrl(5)).get().futureValue
        response.status mustBe Status.OK

        val organisation = verifyNode[Organisation](
          response, OrganisationType, "My Org1", 5
        )
        organisation mustBe an[Organisation]
      }

      "successfully get a building" in {
        val response = wsUrl(StorageNodeUrl(6)).get().futureValue
        response.status mustBe Status.OK

        val building = verifyNode[Building](
          response, BuildingType, "My Building1", 6, Some(5)
        )
        building mustBe a[Building]
      }

      "successfully get a room" in {
        val response = wsUrl(StorageNodeUrl(7)).get().futureValue
        response.status mustBe Status.OK

        val room = verifyNode[Room](
          response, RoomType, "My Room1", 7, Some(6)
        )
        room mustBe a[Room]
        room.environmentRequirement must not be None
      }

      "successfully get a storage unit" in {
        val response = wsUrl(StorageNodeUrl(8)).get().futureValue
        response.status mustBe Status.OK

        val su = verifyNode[StorageUnit](
          response, StorageUnitType, "My Shelf1", 8, Some(7)
        )
        su mustBe a[StorageUnit]
        su.environmentRequirement must not be None
      }

      "not find a storage node with an invalid Id" in {
        val response = wsUrl(StorageNodeUrl(9999)).get().futureValue
        response.status mustBe Status.NOT_FOUND
      }

      "successfully update a storage unit" in {
        val json = storageUnitJson("My Shelf2", StorageNodeId(7))
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED
        val su = verifyNode[StorageUnit](
          response, StorageUnitType, "My Shelf2", 9, Some(7)
        )
        su mustBe a[StorageUnit]
        su.areaTo mustBe Some(.5)
        su.heightTo mustBe Some(.6)

        val updatedJson = {
          Json.parse(response.body).as[JsObject] ++ Json.obj(
            "name" -> "My Shelf2b",
            "areaTo" -> JsNumber(.8),
            "heightTo" -> JsNumber(.8)
          )
        }

        val updRes = wsUrl(StorageNodeUrl(su.id.get)).put(updatedJson).futureValue
        updRes.status mustBe Status.OK
        val updated = verifyNode[StorageUnit](
          updRes, StorageUnitType, "My Shelf2b", su.id.get, Some(7)
        )

        updated mustBe a[StorageUnit]
        updated.areaTo mustBe Some(.8)
        updated.heightTo mustBe Some(.8)
      }

      "successfully update a room" in {
        val json = roomJson("My Room2", StorageNodeId(6))
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED
        val room = verifyNode[Room](
          response, RoomType, "My Room2", 10, Some(6)
        )
        room mustBe a[Room]
        room.areaTo mustBe Some(21.0)
        room.heightTo mustBe Some(2.6)

        val updatedJson = {
          Json.parse(response.body).as[JsObject] ++ Json.obj(
            "name" -> "My Room2b",
            "lightingCondition" -> true
          )
        }

        val updRes = wsUrl(StorageNodeUrl(room.id.get)).put(updatedJson).futureValue
        updRes.status mustBe Status.OK
        val updated = verifyNode[Room](
          updRes, RoomType, "My Room2b", room.id.get, Some(6)
        )

        updated mustBe a[Room]
        updated.environmentAssessment.lightingCondition mustBe Some(true)
      }

      "successfully update a building with environment requirements" in {
        val json = buildingJson("My Building2", StorageNodeId(5))
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED
        val building = verifyNode[Building](
          response, BuildingType, "My Building2", 11, Some(5)
        )
        building mustBe a[Building]
        building.areaTo mustBe Some(210.0)
        building.heightTo mustBe Some(3.5)

        val updatedJson = {
          Json.parse(response.body).as[JsObject] ++ Json.obj(
            "address" -> "Fjære Åker Øya 21, 2341 Huttiheita, Norge",
            "environmentRequirement" -> Json.parse(envReqJson("Filthy"))
          )
        }

        val updRes = wsUrl(StorageNodeUrl(building.id.get)).put(updatedJson).futureValue
        updRes.status mustBe Status.OK
        val updated = verifyNode[Building](
          updRes, BuildingType, "My Building2", building.id.get, Some(5)
        )

        updated mustBe a[Building]
        updated.address mustBe Some("Fjære Åker Øya 21, 2341 Huttiheita, Norge")
        updated.environmentRequirement.isEmpty must not be true
        updated.environmentRequirement.get.cleaning mustBe Some("Filthy")
      }

      "successfully update an organisation" in {
        val json = organisationJson("My Organisation2")
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED
        val organisation = verifyNode[Organisation](
          response, OrganisationType, "My Organisation2", 12
        )
        organisation mustBe an[Organisation]
        organisation.areaTo mustBe Some(2100)
        organisation.heightTo mustBe Some(3.5)

        val updatedJson = {
          Json.parse(response.body).as[JsObject] ++ Json.obj(
            "address" -> "Fjære Åker Øya 21, 2341 Huttiheita, Norge"
          )
        }

        val updRes = wsUrl(StorageNodeUrl(organisation.id.get)).put(updatedJson).futureValue
        updRes.status mustBe Status.OK
        val updated = verifyNode[Organisation](
          updRes, OrganisationType, "My Organisation2", organisation.id.get
        )

        updated mustBe an[Organisation]
        updated.address mustBe Some("Fjære Åker Øya 21, 2341 Huttiheita, Norge")
      }

      "respond with 404 when trying to update a node that doesn't exist" in {
        val json = storageUnitJson("Non existent", StorageNodeId(3))

        val failedUpdate = wsUrl(StorageNodeUrl(999)).put(json).futureValue
        failedUpdate.status mustBe Status.NOT_FOUND
      }

      "successfully delete a storage node" in {
        val json = storageUnitJson("Remove me", StorageNodeId(7))
        val res = wsUrl(StorageNodesUrl).post(json).futureValue
        val created = verifyNode[StorageUnit](
          res, StorageUnitType, "Remove me", 13, Some(7)
        )

        created mustBe a[StorageUnit]

        val rmRes = wsUrl(StorageNodeUrl(created.id.get)).delete().futureValue
        rmRes.status mustBe Status.OK

        val notFound = wsUrl(StorageNodeUrl(created.id.get)).get().futureValue
        notFound.status mustBe Status.NOT_FOUND
      }

      "respond with 404 when deleting a node that doesn't exist" in {
        val rmRes = wsUrl(StorageNodeUrl(9999)).delete().futureValue
        rmRes.status mustBe Status.NOT_FOUND
      }

      "respond with 404 when deleting a node that is already deleted" in {
        val rmRes = wsUrl(StorageNodeUrl(13)).delete().futureValue
        rmRes.status mustBe Status.NOT_FOUND
      }

      "respond with 404 when updating a node that is deleted" in {
        val json = {
          storageUnitJson("Remove me", StorageNodeId(7)).as[JsObject] ++ Json.obj(
            "id" -> 13,
            "name" -> "Hakuna Matata"
          )
        }

        val failedUpdate = wsUrl(StorageNodeUrl(13)).put(json).futureValue
        failedUpdate.status mustBe Status.NOT_FOUND
      }

      "successfully move a single node" in {
        val json = storageUnitJson("Move me", StorageNodeId(7))
        val res = wsUrl(StorageNodesUrl).post(json).futureValue
        val created = verifyNode[StorageUnit](
          res, StorageUnitType, "Move me", 14, Some(7)
        )

        created mustBe a[StorageUnit]

        val moveMeId = created.id.get.underlying

        val moveJson = Json.parse(
          s"""{
             |  "doneBy": 1,
             |  "destination": 9,
             |  "items": [${created.id.get.underlying}]
             |}""".stripMargin)

        val moveRes = wsUrl(MoveStorageNodeUrl).put(moveJson).futureValue
        moveRes.status mustBe Status.OK

        (moveRes.json \ "moved").as[JsArray].value.head.as[Long] mustBe moveMeId

        val movedNodeRes = wsUrl(StorageNodeUrl(moveMeId)).get().futureValue

        println(Json.prettyPrint(movedNodeRes.json))

        val moved = verifyNode[StorageUnit](
          movedNodeRes, StorageUnitType, "Move me", moveMeId, Some(9)
        )
        moved mustBe a[StorageUnit]

      }

      "successfully move several nodes" in {
        val json1 = storageUnitJson("Move me1", StorageNodeId(7))
        val res1 = wsUrl(StorageNodesUrl).post(json1).futureValue
        val json2 = storageUnitJson("Move me2", StorageNodeId(7))
        val res2 = wsUrl(StorageNodesUrl).post(json2).futureValue
        val json3 = storageUnitJson("Move me3", StorageNodeId(7))
        val res3 = wsUrl(StorageNodesUrl).post(json3).futureValue

        res1.status mustBe Status.CREATED
        res2.status mustBe Status.CREATED
        res3.status mustBe Status.CREATED

        val id1 = (res1.json \ "id").as[Long]
        val id2 = (res2.json \ "id").as[Long]
        val id3 = (res3.json \ "id").as[Long]

        val moveJson = Json.parse(
          s"""{
              |  "doneBy": 1,
              |  "destination": 9,
              |  "items": [$id1, $id2, $id3]
              |}""".stripMargin)

        val move = wsUrl(MoveStorageNodeUrl).put(moveJson).futureValue

        move.status mustBe Status.OK

        val movedRes1 = wsUrl(StorageNodeUrl(id1)).get().futureValue
        val movedRes2 = wsUrl(StorageNodeUrl(id2)).get().futureValue
        val movedRes3 = wsUrl(StorageNodeUrl(id3)).get().futureValue

        verifyNode[StorageUnit](
          movedRes1, StorageUnitType, "Move me1", id1, Some(9)
        )
        verifyNode[StorageUnit](
          movedRes2, StorageUnitType, "Move me2", id2, Some(9)
        )
        verifyNode[StorageUnit](
          movedRes3, StorageUnitType, "Move me3", id3, Some(9)
        )

      }

      "successfully move several objects" in {
        val id1 = 1234
        val id2 = 5678
        val id3 = 9876

        val moveJson = Json.parse(
          s"""{
              |  "doneBy": 1,
              |  "destination": 9,
              |  "items": [$id1, $id2, $id3]
              |}""".stripMargin)

        val move = wsUrl(MoveObjectUrl).put(moveJson).futureValue

        move.status mustBe Status.OK

        (move.json \ "moved").as[JsArray].value.map(_.as[Int]) must contain allOf(id1, id2, id3)
      }

      "update should fail if type doesn't match" in {
        //        val json = buildingJson("My Building10", StorageNodeId(1))
        //        val response = postStorageNode(json).futureValue
        //        response.status mustBe Status.CREATED
        //        val building = verifyNode[Building](
        //          response, BuildingType, "My Building10", 10, Some(1)
        //        )
        //        building mustBe a[Building]
        //        building.areaTo mustBe Some(210.0)
        //        building.heightTo mustBe Some(3.5)
        //
        //        val updatedJson = {
        //          Json.parse(response.body).as[JsObject] ++ Json.obj(
        //            "type" -> "Organisation"
        //          )
        //        }
        //
        //        val updRes = putStorageNode(building.id.get, updatedJson).futureValue
        //        updRes.status mustBe Status.BAD_REQUEST
        //
        // TODO: This test is pending until it's been clarified if NotFound is
        // a proper response in this case. I would argue that it is. Since there
        // is no node in the DB matching the ID with the wrong type
        pending
      }
    }
  }
}