package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.GeoLocationResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader

class GeoLocationRestMapperTest {

    private lateinit var mapper: GeoLocationRestMapper
    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        mapper = GeoLocationRestMapper()
    }

    @Test
    fun `fromIGeoLocationModelToResponse should map ContinentModel correctly`() {
        val model = ContinentModel(id = GeoLocationId(1), friendlyId = "europe", name = "Europe", boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val response = mapper.fromIGeoLocationModelToResponse(model, true)

        assertEquals(model.id.id, response.id)
        assertEquals(model.friendlyId, response.friendlyId)
        assertEquals(model.name, response.name)
        assertEquals(model.type, response.type)
        assertEquals(model.boundaryRepresentation?.toText(), response.boundaryRepresentation)
        assertNull(response.parentId)
    }

    @Test
    fun `fromCreateGeoLocationRequestToModel should map Continent correctly`() {
        val request = CreateGeoLocationRequest(friendlyId = "africa", name = "Africa", boundaryRepresentation = "POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))")
        val model = mapper.fromCreateGeoLocationRequestToModel(GeoLocationType.CONTINENT, request)

        assertTrue(model is ContinentModel)
        assertEquals(request.name, model.name)
        assertEquals(request.friendlyId, model.friendlyId)
        assertEquals(request.boundaryRepresentation?.let { wktReader.read(it) }, model.boundaryRepresentation)
    }

    @Test
    fun `fromCreateGeoLocationRequestToModel should map Region correctly with parent`() {
        val parentContinent = ContinentModel(id = GeoLocationId(100), friendlyId = "asia", name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val request = CreateGeoLocationRequest(friendlyId = "east-asia", name = "East Asia", parentId = 100, boundaryRepresentation = "POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))")
        val model = mapper.fromCreateGeoLocationRequestToModel(GeoLocationType.REGION, request, parentContinent)

        assertTrue(model is RegionModel)
        assertEquals(request.name, model.name)
        assertEquals(request.friendlyId, model.friendlyId)
        assertEquals(request.boundaryRepresentation?.let { wktReader.read(it) }, model.boundaryRepresentation)
        assertEquals(parentContinent, (model as RegionModel).continent)
    }

    @Test
    fun `fromIGeoLocationModelToResponse should map RegionModel correctly`() {
        val continentModel = ContinentModel(id = GeoLocationId(100), friendlyId = "asia", name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val model = RegionModel(id = GeoLocationId(1), friendlyId = "southeast-asia", name = "Southeast Asia", additionalDetailsMap = null, parentId = continentModel.id.id, continent = continentModel, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), countriesList = emptyList())
        val response = mapper.fromIGeoLocationModelToResponse(model, true)

        assertEquals(model.id.id, response.id)
        assertEquals(model.friendlyId, response.friendlyId)
        assertEquals(model.name, response.name)
        assertEquals(model.type, response.type)
        assertEquals(model.boundaryRepresentation?.toText(), response.boundaryRepresentation)
        assertEquals(model.continent.id.id, response.parentId)
    }
}