package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.ContinentModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ContinentEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ContinentJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader
import java.util.*

class ContinentPersistenceJpaAdapterTest {

    private val continentJpaRepository: ContinentJpaRepository = mockk()
    private val geoLocationJpaMapper: GeoLocationJpaMapper = mockk()

    private lateinit var adapter: ContinentPersistenceJpaAdapter

    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        adapter = ContinentPersistenceJpaAdapter(continentJpaRepository, geoLocationJpaMapper)
    }

    @Test
    fun `saveNew should save and return ContinentModel`() {
        val model = ContinentModel(id = GeoLocationId(0), name = "Asia", boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val entity = ContinentEntity(name = "Asia", boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val savedEntity = ContinentEntity(name = "Asia", boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val savedModel = ContinentModel(id = GeoLocationId(1), name = "Asia", boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())

        every { geoLocationJpaMapper.continentToJpaEntity(model) } returns entity
        every { continentJpaRepository.save(entity) } returns savedEntity
        every { geoLocationJpaMapper.continentToDomainModel(savedEntity) } returns savedModel

        val result = adapter.saveNew(model)

        assertEquals(savedModel, result)
        verify(exactly = 1) { continentJpaRepository.save(entity) }
    }

    @Test
    fun `update should update and return ContinentModel`() {
        val model = ContinentModel(id = GeoLocationId(1), name = "Updated Asia", boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"), regionsList = emptyList())
        val existingEntity = ContinentEntity(name = "Asia", boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), regionsList = emptyList())
        val updatedEntity = ContinentEntity(name = "Updated Asia", boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"), regionsList = emptyList())
        val updatedModel = ContinentModel(id = GeoLocationId(1), name = "Updated Asia", boundaryRepresentation = wktReader.read("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"), regionsList = emptyList())

        every { continentJpaRepository.findById(1L) } returns Optional.of(existingEntity)
        every { continentJpaRepository.save(existingEntity) } returns updatedEntity
        every { geoLocationJpaMapper.continentToDomainModel(updatedEntity) } returns updatedModel

        val result = adapter.update(model)

        assertEquals(updatedModel, result)
        verify(exactly = 1) { continentJpaRepository.findById(1L) }
        verify(exactly = 1) { continentJpaRepository.save(existingEntity) }
        assertEquals("Updated Asia", existingEntity.name)
    }

    @Test
    fun `findById should return ContinentModel if found`() {
        val entity = ContinentEntity(name = "Asia", boundaryRepresentation = null, regionsList = emptyList())
        val model = ContinentModel(id = GeoLocationId(1), name = "Asia", boundaryRepresentation = null, regionsList = emptyList())

        every { continentJpaRepository.findById(1L) } returns Optional.of(entity)
        every { geoLocationJpaMapper.continentToDomainModel(entity) } returns model

        val result = adapter.findById(GeoLocationId(1))

        assertTrue(result.isPresent)
        assertEquals(model, result.get())
    }

    @Test
    fun `findAll should return list of ContinentModels`() {
        val entities = listOf(ContinentEntity(name = "Asia", boundaryRepresentation = null, regionsList = emptyList()))
        val models = listOf(ContinentModel(id = GeoLocationId(1), name = "Asia", boundaryRepresentation = null, regionsList = emptyList()))

        every { continentJpaRepository.findAll() } returns entities
        every { geoLocationJpaMapper.continentToDomainModel(any()) } answers { models[0] }

        val result = adapter.findAll()

        assertEquals(models, result)
    }

    @Test
    fun `deleteById should delete Continent`() {
        every { continentJpaRepository.deleteById(1L) } returns Unit

        adapter.deleteById(GeoLocationId(1))

        verify(exactly = 1) { continentJpaRepository.deleteById(1L) }
    }

    @Test
    fun `findByNameStartingWith should return filtered list`() {
        val entities = listOf(ContinentEntity(name = "Asia", boundaryRepresentation = null, regionsList = emptyList()))
        val models = listOf(ContinentModel(id = GeoLocationId(1), name = "Asia", boundaryRepresentation = null, regionsList = emptyList()))

        every { continentJpaRepository.findByNameStartingWithIgnoreCase("A") } returns entities
        every { geoLocationJpaMapper.continentToDomainModel(any()) } answers { models[0] }

        val result = adapter.findByNameStartingWith("A")

        assertEquals(models, result)
    }
}