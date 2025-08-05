package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ContinentModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.GeoLocationResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.DeepGeoLocationRestMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.GeoLocationRestMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader
import org.springframework.http.HttpStatus
import java.util.*

class GeoLocationControllerTest {

    private val geoLocationCRUDService: GeoLocationCRUDService = mockk()
    private val geoLocationRestMapper: GeoLocationRestMapper = mockk()
    private val deepGeoLocationRestMapper: DeepGeoLocationRestMapper = mockk()

    private lateinit var controller: GeoLocationController

    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        controller = GeoLocationController(geoLocationCRUDService, geoLocationRestMapper, deepGeoLocationRestMapper)
    }

    @Test
    fun `create should return created GeoLocation`() {
        val type = GeoLocationType.CONTINENT
        val request = CreateGeoLocationRequest(friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))")
        val continentModel = ContinentModel(id = GeoLocationId(1), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"), regionsList = emptyList())
        val responseDto = GeoLocationResponse(type = type, id = 1, friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))", parentId = null)

        every { geoLocationCRUDService.create(type, request) } returns continentModel
        every { geoLocationRestMapper.fromIGeoLocationModelToResponse(continentModel, true) } returns responseDto

        val response = controller.create(type.name, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(responseDto, response.body)
        verify(exactly = 1) { geoLocationCRUDService.create(type, request) }
    }

    @Test
    fun `findById should return GeoLocation if found`() {
        val type = GeoLocationType.CONTINENT
        val id = 1L
        val continentModel = ContinentModel(id = GeoLocationId(id), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList())
        val responseDto = GeoLocationResponse(type = type, id = id, friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, parentId = null)

        every { geoLocationCRUDService.findById(type, id) } returns Optional.of(continentModel)
        every { geoLocationRestMapper.fromIGeoLocationModelToResponse(continentModel, false) } returns responseDto

        val response = controller.findById(type.name, id, false)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(responseDto, response.body)
        verify(exactly = 1) { geoLocationCRUDService.findById(type, id) }
    }

    @Test
    fun `findAll should return list of GeoLocations`() {
        val type = GeoLocationType.CONTINENT
        val continents = listOf(
            ContinentModel(id = GeoLocationId(1), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList()),
            ContinentModel(id = GeoLocationId(2), friendlyId = "europe", name = "Europe", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList())
        )
        val responseList = listOf(
            GeoLocationResponse(type = type, id = 1, friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, parentId = null),
            GeoLocationResponse(type = type, id = 2, friendlyId = "europe", name = "Europe", additionalDetailsMap = null, boundaryRepresentation = null, parentId = null)
        )

        every { geoLocationCRUDService.findAll(type) } returns continents
        every { geoLocationRestMapper.fromIGeoLocationModelToResponse(any(), false) } answers { callOriginal() }

        val result = controller.findAll(type.name, false)

        assertEquals(2, result.body?.size)
        // Note: Direct comparison of lists of complex objects might fail if equals/hashCode are not properly implemented
        // For simplicity, we're assuming they are for this test.
        // A more robust test would compare properties individually or use a custom matcher.
        assertEquals(responseList[0].id, result.body?.get(0)?.id)
        assertEquals(responseList[0].name, result.body?.get(0)?.name)
        assertEquals(responseList[1].id, result.body?.get(1)?.id)
        assertEquals(responseList[1].name, result.body?.get(1)?.name)
        verify(exactly = 1) { geoLocationCRUDService.findAll(type) }
    }

    @Test
    fun `update should return updated GeoLocation`() {
        val type = GeoLocationType.CONTINENT
        val id = 1L
        val request = UpdateGeoLocationRequest(friendlyId = "asia-updated", name = "Updated Asia", additionalDetailsMap = null, boundaryRepresentation = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))")
        val existingModel = ContinentModel(id = GeoLocationId(id), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList())
        val updatedModel = existingModel.copy(friendlyId = "asia-updated", name = "Updated Asia", boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"))
        val responseDto = GeoLocationResponse(type = type, id = id, friendlyId = "asia-updated", name = "Updated Asia", additionalDetailsMap = null, boundaryRepresentation = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))", parentId = null)

        every { geoLocationCRUDService.update(type, id, request) } returns updatedModel
        every { geoLocationRestMapper.fromIGeoLocationModelToResponse(updatedModel, true) } returns responseDto

        val response = controller.update(type.name, id, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(responseDto, response.body)
        verify(exactly = 1) { geoLocationCRUDService.update(type, id, request) }
    }

    @Test
    fun `deleteById should return no content`() {
        val type = GeoLocationType.CONTINENT
        val id = 1L

        every { geoLocationCRUDService.deleteById(type, id) } returns Unit

        val response = controller.deleteById(type.name, id)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify(exactly = 1) { geoLocationCRUDService.deleteById(type, id) }
    }

    @Test
    fun `search should return list of GeoLocations`() {
        val type = GeoLocationType.CONTINENT
        val namePrefix = "A"
        val continents = listOf(ContinentModel(id = GeoLocationId(1), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList()))
        val responseList = listOf(GeoLocationResponse(type = type, id = 1, friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, parentId = null))

        every { geoLocationCRUDService.searchByParentIdAndNameStartingWith(type, null, namePrefix) } returns continents
        every { geoLocationRestMapper.fromIGeoLocationModelToResponse(any(), false) } answers { callOriginal() }

        val result = controller.searchByParentIdAndNameStartingWith(type.name, null, namePrefix, false)

        assertEquals(1, result.body?.size)
        assertEquals(responseList[0].id, result.body?.get(0)?.id)
        assertEquals(responseList[0].name, result.body?.get(0)?.name)
        verify(exactly = 1) { geoLocationCRUDService.searchByParentIdAndNameStartingWith(type, null, namePrefix) }
    }
}
