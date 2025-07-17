package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.ContinentModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.RegionModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ContinentEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.RegionEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ContinentJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.RegionJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader
import java.util.*

class RegionPersistenceJpaAdapterTest {

    private val regionJpaRepository: RegionJpaRepository = mockk()
    private val continentJpaRepository: ContinentJpaRepository = mockk()
    private val geoLocationJpaMappers: GeoLocationJpaMappers = mockk()
    private val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers = mockk()

    private lateinit var adapter: RegionPersistenceJpaAdapter

    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        adapter = RegionPersistenceJpaAdapter(regionJpaRepository, continentJpaRepository, geoLocationJpaMappers, deepGeoLocationJpaMappers)
    }

    @Test
    fun `saveNew should save and return RegionModel`() {
        val continentId = 1L
        val continentEntity = ContinentEntity(name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val continentModel = ContinentModel(id = GeoLocationId(continentId), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        every { geoLocationJpaMappers.continentToDomainModel(continentEntity) } returns continentModel
        val model = RegionModel(id = GeoLocationId(0), name = "Southeast Asia", parentId = continentEntity.id, continent = geoLocationJpaMappers.continentToDomainModel(continentEntity), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), countriesList = emptyList())
        val regionEntity = RegionEntity(name = "Southeast Asia", continent = continentEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), countriesList = emptyList())
        val savedEntity = RegionEntity(name = "Southeast Asia", continent = continentEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), countriesList = emptyList())
        val savedModel = RegionModel(id = GeoLocationId(2), name = "Southeast Asia", parentId = continentEntity.id, continent = geoLocationJpaMappers.continentToDomainModel(continentEntity), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), countriesList = emptyList())

        every { continentJpaRepository.findById(continentId) } returns Optional.of(continentEntity)
        every { regionJpaRepository.save(any<RegionEntity>()) } returns savedEntity
        every { geoLocationJpaMappers.regionToDomainModel(savedEntity) } returns savedModel

        val result = adapter.saveNew(model)

        assertEquals(savedModel, result)
        verify(exactly = 1) { regionJpaRepository.save(any<RegionEntity>()) }
    }

    @Test
    fun `update should update and return RegionModel`() {
        val continentId = 1L
        val regionId = 2L
        val continentEntity = ContinentEntity(name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val continentModel = ContinentModel(id = GeoLocationId(continentId), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        every { geoLocationJpaMappers.continentToDomainModel(continentEntity) } returns continentModel
        val model = RegionModel(id = GeoLocationId(regionId), name = "Updated Southeast Asia", parentId = continentEntity.id, continent = geoLocationJpaMappers.continentToDomainModel(continentEntity), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), countriesList = emptyList())
        val existingEntity = RegionEntity(name = "Southeast Asia", continent = continentEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), countriesList = emptyList())
        val updatedEntity = RegionEntity( name = "Updated Southeast Asia", continent = continentEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedModel = RegionModel(id = GeoLocationId(regionId), name = "Updated Southeast Asia", parentId = continentEntity.id, continent = geoLocationJpaMappers.continentToDomainModel(continentEntity), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))

        every { regionJpaRepository.findById(regionId) } returns Optional.of(existingEntity)
        every { continentJpaRepository.findById(continentId) } returns Optional.of(continentEntity)
        every { regionJpaRepository.save(existingEntity) } returns updatedEntity
        every { geoLocationJpaMappers.regionToDomainModel(updatedEntity) } returns updatedModel

        val result = adapter.update(model)

        assertEquals(updatedModel, result)
        verify(exactly = 1) { regionJpaRepository.findById(regionId) }
        verify(exactly = 1) { regionJpaRepository.save(existingEntity) }
        assertEquals("Updated Southeast Asia", existingEntity.name)
    }

    @Test
    fun `findById should return RegionModel if found`() {
        val continentId = 1L
        val regionId = 2L
        val continentEntity = ContinentEntity(name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val continentModel = ContinentModel(id = GeoLocationId(continentId), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        every { geoLocationJpaMappers.continentToDomainModel(continentEntity) } returns continentModel
        val entity = RegionEntity(name = "Southeast Asia", continent = continentEntity, boundaryRepresentation = null, countriesList = emptyList())
        val model = RegionModel(id = GeoLocationId(regionId), name = "Southeast Asia", parentId = continentEntity.id, continent = geoLocationJpaMappers.continentToDomainModel(continentEntity), boundaryRepresentation = null, countriesList = emptyList())

        every { regionJpaRepository.findById(regionId) } returns Optional.of(entity)
        every { geoLocationJpaMappers.regionToDomainModel(entity) } returns model

        val result = adapter.findById(GeoLocationId(regionId))

        assertTrue(result.isPresent)
        assertEquals(model, result.get())
    }

    @Test
    fun `findAll should return list of RegionModels`() {
        val continentId = 1L
        val continentEntity = ContinentEntity(name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val entities = listOf(RegionEntity(name = "Southeast Asia", continent = continentEntity, boundaryRepresentation = null, countriesList = emptyList()))
        val continentModel = ContinentModel(id = GeoLocationId(continentId), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        every { geoLocationJpaMappers.continentToDomainModel(continentEntity) } returns continentModel
        val models = listOf(RegionModel(id = GeoLocationId(2), name = "Southeast Asia", parentId = continentEntity.id, continent = geoLocationJpaMappers.continentToDomainModel(continentEntity), boundaryRepresentation = null, countriesList = emptyList()))

        every { regionJpaRepository.findAll() } returns entities
        every { geoLocationJpaMappers.regionToDomainModel(any()) } answers { models[0] }

        val result = adapter.findAll()

        assertEquals(models, result)
    }

    @Test
    fun `deleteById should delete Region`() {
        val regionId = 2L

        every { regionJpaRepository.deleteById(regionId) } returns Unit

        adapter.deleteById(GeoLocationId(regionId))

        verify(exactly = 1) { regionJpaRepository.deleteById(regionId) }
    }

    @Test
    fun `findByContinentIdAndNameStartingWith should return filtered list`() {
        val continentId = 1L
        val namePrefix = "S"
        val continentEntity = ContinentEntity(name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val entities = listOf(RegionEntity(name = "Southeast Asia", continent = continentEntity, boundaryRepresentation = null, countriesList = emptyList()))
        val continentModel = ContinentModel(id = GeoLocationId(continentId), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        every { geoLocationJpaMappers.continentToDomainModel(continentEntity) } returns continentModel
        val models = listOf(RegionModel(id = GeoLocationId(2), name = "Southeast Asia", parentId = continentEntity.id, continent = geoLocationJpaMappers.continentToDomainModel(continentEntity), boundaryRepresentation = null, countriesList = emptyList()))

        every { regionJpaRepository.findByContinentIdAndNameStartingWithIgnoreCase(continentId, namePrefix) } returns entities
        every { geoLocationJpaMappers.regionToDomainModel(any()) } answers { models[0] }

        val result = adapter.findByContinentIdAndNameStartingWith(GeoLocationId(continentId), namePrefix)

        assertEquals(models, result)
    }
}