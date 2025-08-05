package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.ContinentModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ContinentEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ContinentJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.*

class ContinentPersistenceJpaAdapterTest {

    private val continentJpaRepository: ContinentJpaRepository = mockk()
    private val geoLocationJpaMappers: GeoLocationJpaMappers = mockk()
    private val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers = mockk()

    private lateinit var adapter: ContinentPersistenceJpaAdapter

    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        adapter = ContinentPersistenceJpaAdapter(continentJpaRepository, geoLocationJpaMappers, deepGeoLocationJpaMappers)
    }

    @Test
    fun `saveNew should save and return ContinentModel`() {
        val model = ContinentModel(id = GeoLocationId(0), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val entity = ContinentEntity(friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val savedEntity = ContinentEntity(friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val savedModel = ContinentModel(id = GeoLocationId(1), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())

        every { geoLocationJpaMappers.continentToJpaEntity(model) } returns entity
        every { continentJpaRepository.save(entity) } returns savedEntity
        every { geoLocationJpaMappers.continentToDomainModel(savedEntity, true) } returns savedModel

        val result = adapter.saveNew(model)

        assertEquals(savedModel, result)
        verify(exactly = 1) { continentJpaRepository.save(entity) }
    }

    @Test
    fun `update should update and return ContinentModel`() {
        val model = ContinentModel(id = GeoLocationId(1), friendlyId = "updated-asia", name = "Updated Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"), regionsList = emptyList())
        val existingEntity = ContinentEntity(friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val updatedEntity = ContinentEntity(friendlyId = "updated-asia", name = "Updated Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"), regionsList = emptyList())
        val updatedModel = ContinentModel(id = GeoLocationId(1), friendlyId = "updated-asia", name = "Updated Asia", additionalDetailsMap = null, boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"), regionsList = emptyList())

        every { continentJpaRepository.findById(1L) } returns Optional.of(existingEntity)
        every { continentJpaRepository.save(existingEntity) } returns updatedEntity
        every { geoLocationJpaMappers.continentToDomainModel(updatedEntity, true) } returns updatedModel

        val result = adapter.update(model)

        assertEquals(updatedModel, result)
        verify(exactly = 1) { continentJpaRepository.findById(1L) }
        verify(exactly = 1) { continentJpaRepository.save(existingEntity) }
        assertEquals("Updated Asia", existingEntity.name)
    }

    @Test
    fun `findById should return ContinentModel if found`() {
        val entity = ContinentEntity(friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList())
        val model = ContinentModel(id = GeoLocationId(1), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList())

        every { continentJpaRepository.findById(1L) } returns Optional.of(entity)
        every { geoLocationJpaMappers.continentToDomainModel(entity, true) } returns model

        val result = adapter.findById(GeoLocationId(1))

        assertTrue(result.isPresent)
        assertEquals(model, result.get())
    }

    @Test
    fun `findAll should return list of ContinentModels`() {
        val entities = listOf(ContinentEntity(friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList()))
        val pageOfResults = PageImpl(entities)

        val models = listOf(ContinentModel(id = GeoLocationId(1), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList()))

        every { continentJpaRepository.findAll(Pageable.unpaged()) } returns pageOfResults
        every { geoLocationJpaMappers.continentToDomainModel(any(), true) } answers { models[0] }

        val result = adapter.findAll(Pageable.unpaged())

        assertEquals(models, result.content)
    }

    @Test
    fun `deleteById should delete Continent`() {
        every { continentJpaRepository.deleteById(1L) } returns Unit

        adapter.deleteById(GeoLocationId(1))

        verify(exactly = 1) { continentJpaRepository.deleteById(1L) }
    }

    @Test
    fun `findByNameStartingWith should return filtered list`() {
        val entities = listOf(ContinentEntity(friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList()))
        val pageOfResults = PageImpl(entities)

        val models = listOf(ContinentModel(id = GeoLocationId(1), friendlyId = "asia", name = "Asia", additionalDetailsMap = null, boundaryRepresentation = null, regionsList = emptyList()))

        every { continentJpaRepository.findByNameStartingWithIgnoreCase("A", Pageable.unpaged()) } returns pageOfResults
        every { geoLocationJpaMappers.continentToDomainModel(any(), true) } answers { models[0] }

        val result = adapter.findByNameStartingWith("A", Pageable.unpaged())

        assertEquals(models, result.content)
    }
}