package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.*
import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.GeoLocationRestMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTReader
import java.util.*

class GeoLocationCRUDServiceTest {

    private val continentUseCase: ContinentUseCase = mockk()
    private val regionUseCase: RegionUseCase = mockk()
    private val countryUseCase: CountryUseCase = mockk()
    private val provinceUseCase: ProvinceUseCase = mockk()
    private val cityUseCase: CityUseCase = mockk()
    private val districtUseCase: DistrictUseCase = mockk()
    private val geoLocationRestMapper: GeoLocationRestMapper = mockk()
    private val eventPublisherPort: EventPublisherPort = mockk()

    private lateinit var service: GeoLocationCRUDService

    private val geometryFactory = GeometryFactory()
    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        service = GeoLocationCRUDService(
            continentUseCase,
            regionUseCase,
            countryUseCase,
            provinceUseCase,
            cityUseCase,
            districtUseCase,
            geoLocationRestMapper,
            eventPublisherPort
        )
    }

    @Test
    fun `create Continent should call continentUseCase create`() {
        val request = CreateGeoLocationRequest(name = "Asia", boundaryRepresentation = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))")
        val continentModel = ContinentModel(id = GeoLocationId(1), name = "Asia", boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"), regionsList = emptyList())

        every { geoLocationRestMapper.fromCreateGeoLocationRequestToModel(GeoLocationType.CONTINENT, request, null) } returns continentModel
        every { continentUseCase.create(continentModel) } returns continentModel

        val result = service.create(GeoLocationType.CONTINENT, request)

        assertEquals(continentModel, result)
        verify(exactly = 1) { continentUseCase.create(continentModel) }
    }

    @Test
    fun `create Region should call regionUseCase create with parent Continent`() {
        val continentId = 1L
        val request = CreateGeoLocationRequest(name = "Southeast Asia", parentId = continentId, boundaryRepresentation = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))")
        val continentModel = ContinentModel(id = GeoLocationId(continentId), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val regionModel = RegionModel(id = GeoLocationId(2), name = "Southeast Asia", parentId = continentModel.id.id, continent = continentModel, boundaryRepresentation = null, countriesList = emptyList())

        every { continentUseCase.findById(GeoLocationId(continentId)) } returns Optional.of(continentModel)
        every { geoLocationRestMapper.fromCreateGeoLocationRequestToModel(GeoLocationType.REGION, request, continentModel) } returns regionModel
        every { regionUseCase.create(regionModel) } returns regionModel

        val result = service.create(GeoLocationType.REGION, request)

        assertEquals(regionModel, result)
        verify(exactly = 1) { regionUseCase.create(regionModel) }
        verify(exactly = 1) { continentUseCase.findById(GeoLocationId(continentId)) }
    }

    @Test
    fun `findById should return model if found`() {
        val continentId = 1L
        val continentModel = ContinentModel(id = GeoLocationId(continentId), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())

        every { continentUseCase.findById(GeoLocationId(continentId)) } returns Optional.of(continentModel)

        val result = service.findById(GeoLocationType.CONTINENT, continentId)

        assertTrue(result.isPresent)
        assertEquals(continentModel, result.get())
        verify(exactly = 1) { continentUseCase.findById(GeoLocationId(continentId)) }
    }

    @Test
    fun `findAll should return all models`() {
        val continents = listOf(
            ContinentModel(id = GeoLocationId(1), name = "Asia", boundaryRepresentation = null, regionsList = emptyList()),
            ContinentModel(id = GeoLocationId(2), name = "Europe", boundaryRepresentation = null, regionsList = emptyList())
        )

        every { continentUseCase.findAll() } returns continents

        val result = service.findAll(GeoLocationType.CONTINENT)

        assertEquals(2, result.size)
        assertEquals(continents, result)
        verify(exactly = 1) { continentUseCase.findAll() }
    }

    @Test
    fun `update Continent should call continentUseCase update`() {
        val continentId = 1L
        val request = UpdateGeoLocationRequest(name = "Updated Asia", boundaryRepresentation = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))")
        val existingContinent = ContinentModel(id = GeoLocationId(continentId), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val updatedContinent = existingContinent.copy(name = "Updated Asia", boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"))

        every { continentUseCase.findById(GeoLocationId(continentId)) } returns Optional.of(existingContinent)
        every { continentUseCase.update(any()) } returns updatedContinent

        val result = service.update(GeoLocationType.CONTINENT, continentId, request)

        assertNotNull(result)
        assertEquals(updatedContinent, result)
        verify(exactly = 1) { continentUseCase.update(any()) }
    }

    @Test
    fun `deleteById should call useCase deleteById`() {
        val continentId = 1L

        every { continentUseCase.deleteById(GeoLocationId(continentId)) } returns Unit

        service.deleteById(GeoLocationType.CONTINENT, continentId)

        verify(exactly = 1) { continentUseCase.deleteById(GeoLocationId(continentId)) }
    }

    @Test
    fun searchByParentIdAndNameStartingWith() {
        val namePrefix = "A"
        val continents = listOf(ContinentModel(id = GeoLocationId(1), name = "Asia", boundaryRepresentation = null, regionsList = emptyList()))

        every { continentUseCase.findByParentIdAndNameStartingWith(null, namePrefix) } returns continents

        val result = service.searchByParentIdAndNameStartingWith(GeoLocationType.CONTINENT, null, namePrefix)

        assertEquals(continents, result)
        verify(exactly = 1) { continentUseCase.findByParentIdAndNameStartingWith(null, namePrefix) }
    }
}
