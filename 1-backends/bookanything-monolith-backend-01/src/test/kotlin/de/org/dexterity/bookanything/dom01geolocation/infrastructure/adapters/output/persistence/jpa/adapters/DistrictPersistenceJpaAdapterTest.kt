package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.DistrictModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.CityModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.DistrictEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CityEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CityJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.DistrictJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader
import java.util.*

class DistrictPersistenceJpaAdapterTest {

    private val districtJpaRepository: DistrictJpaRepository = mockk()
    private val cityJpaRepository: CityJpaRepository = mockk()
    private val geoLocationJpaMappers: GeoLocationJpaMappers = mockk()
    private val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers = mockk()

    private lateinit var adapter: DistrictPersistenceJpaAdapter

    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        adapter = DistrictPersistenceJpaAdapter(districtJpaRepository, cityJpaRepository, geoLocationJpaMappers, deepGeoLocationJpaMappers)
    }

    @Test
    fun `saveNew should save and return DistrictModel`() {
        val cityId = 1L
        val cityModel = CityModel(id = GeoLocationId(cityId), friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, parentId = 1L, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val cityEntity = CityEntity(friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val model = DistrictModel(id = GeoLocationId(0), friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, parentId = cityModel.id.id, city = cityModel, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), addressesList = emptyList())
        val districtEntity = DistrictEntity(friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, city = cityEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), addressesList = emptyList())
        val savedEntity = DistrictEntity(friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, city = cityEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val savedModel = DistrictModel(id = GeoLocationId(2), friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, parentId = cityModel.id.id, city = cityModel, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), addressesList = emptyList())

        every { cityJpaRepository.findById(cityId) } returns Optional.of(cityEntity)
        every { districtJpaRepository.save(any<DistrictEntity>()) } returns savedEntity
        every { geoLocationJpaMappers.districtToDomainModel(savedEntity, true) } returns savedModel

        val result = adapter.saveNew(model)

        assertEquals(savedModel, result)
        verify(exactly = 1) { districtJpaRepository.save(any<DistrictEntity>()) }
    }

    @Test
    fun `update should update and return DistrictModel`() {
        val cityId = 1L
        val districtId = 2L
        val cityModel = CityModel(id = GeoLocationId(cityId), friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, parentId = 1L, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val cityEntity = CityEntity(friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val model = DistrictModel(id = GeoLocationId(districtId), friendlyId = "updated-sukhumvit", name = "Updated Sukhumvit", additionalDetailsMap = null, parentId = cityModel.id.id, city = cityModel, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), addressesList = emptyList())
        val existingEntity = DistrictEntity(friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, city = cityEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedEntity = DistrictEntity(friendlyId = "updated-sukhumvit", name = "Updated Sukhumvit", additionalDetailsMap = null, city = cityEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedModel = DistrictModel(id = GeoLocationId(districtId), friendlyId = "updated-sukhumvit", name = "Updated Sukhumvit", additionalDetailsMap = null, parentId = cityModel.id.id, city = cityModel, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))

        every { districtJpaRepository.findById(districtId) } returns Optional.of(existingEntity)
        every { cityJpaRepository.findById(cityId) } returns Optional.of(cityEntity)
        every { districtJpaRepository.save(existingEntity) } returns updatedEntity
        every { geoLocationJpaMappers.districtToDomainModel(updatedEntity, true) } returns updatedModel

        val result = adapter.update(model)

        assertEquals(updatedModel, result)
        verify(exactly = 1) { districtJpaRepository.findById(districtId) }
        verify(exactly = 1) { districtJpaRepository.save(existingEntity) }
        assertEquals("Updated Sukhumvit", existingEntity.name)
    }

    @Test
    fun `findById should return DistrictModel if found`() {
        val cityId = 1L
        val districtId = 2L
        val cityModel = CityModel(id = GeoLocationId(cityId), friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, parentId = 1L, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val cityEntity = CityEntity(friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val entity = DistrictEntity(friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, city = cityEntity, boundaryRepresentation = null, addressesList = emptyList())
        val model = DistrictModel(id = GeoLocationId(districtId), friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, parentId = cityModel.id.id, city = cityModel, boundaryRepresentation = null, addressesList = emptyList())

        every { districtJpaRepository.findById(districtId) } returns Optional.of(entity)
        every { geoLocationJpaMappers.districtToDomainModel(entity, true) } returns model

        val result = adapter.findById(GeoLocationId(districtId))

        assertTrue(result.isPresent)
        assertEquals(model, result.get())
    }

    @Test
    fun `findAll should return list of DistrictModels`() {
        val cityId = 1L
        val cityModel = CityModel(id = GeoLocationId(cityId), friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, parentId = 1L, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val cityEntity = CityEntity(friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val entities = listOf(DistrictEntity(friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, city = cityEntity, boundaryRepresentation = null, addressesList = emptyList()))
        val models = listOf(DistrictModel(id = GeoLocationId(2), friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, parentId = cityModel.id.id, city = cityModel, boundaryRepresentation = null, addressesList = emptyList()))

        every { districtJpaRepository.findAll() } returns entities
        every { geoLocationJpaMappers.districtToDomainModel(any(), true) } answers { models[0] }

        val result = adapter.findAll()

        assertEquals(models, result)
    }

    @Test
    fun `deleteById should delete District`() {
        val districtId = 2L

        every { districtJpaRepository.deleteById(districtId) } returns Unit

        adapter.deleteById(GeoLocationId(districtId))

        verify(exactly = 1) { districtJpaRepository.deleteById(districtId) }
    }

    @Test
    fun `findByCityIdAndNameStartingWith should return filtered list`() {
        val cityId = 1L
        val namePrefix = "S"
        val cityModel = CityModel(id = GeoLocationId(cityId), friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, parentId = 1L, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val cityEntity = CityEntity(friendlyId = "bangkok-city", name = "Bangkok City", additionalDetailsMap = null, province = mockk(), boundaryRepresentation = null, districtsList = emptyList())
        val entities = listOf(DistrictEntity(friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, city = cityEntity, boundaryRepresentation = null, addressesList = emptyList()))
        val models = listOf(DistrictModel(id = GeoLocationId(2), friendlyId = "sukhumvit", name = "Sukhumvit", additionalDetailsMap = null, parentId = cityModel.id.id, city = cityModel, boundaryRepresentation = null, addressesList = emptyList()))

        every { cityJpaRepository.findById(cityId) } returns Optional.of(cityEntity)
        every { districtJpaRepository.findByCityIdAndNameStartingWithIgnoreCase(cityId, namePrefix) } returns entities
        every { geoLocationJpaMappers.districtToDomainModel(any(), true) } answers { models[0] }

        val result = adapter.findByCityIdAndNameStartingWith(GeoLocationId(cityId), namePrefix)

        assertEquals(models, result)
    }
}