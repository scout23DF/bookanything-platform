package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CountryModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ProvinceModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CountryEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ProvinceEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CountryJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ProvinceJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader
import java.util.*

class ProvincePersistenceJpaAdapterTest {

    private val provinceJpaRepository: ProvinceJpaRepository = mockk()
    private val countryJpaRepository: CountryJpaRepository = mockk()
    private val geoLocationJpaMappers: GeoLocationJpaMappers = mockk()
    private val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers = mockk()

    private lateinit var adapter: ProvincePersistenceJpaAdapter

    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        adapter = ProvincePersistenceJpaAdapter(provinceJpaRepository, countryJpaRepository, geoLocationJpaMappers, deepGeoLocationJpaMappers)
    }

    @Test
    fun `saveNew should save and return ProvinceModel`() {
        val countryId = 1L
        val countryEntity = CountryEntity(friendlyId = "thailand", name = "Thailand", region = mockk(), boundaryRepresentation = null, provincesList = emptyList())

        val countryModel = CountryModel(id = GeoLocationId(countryId), friendlyId = "thailand", name = "Thailand", additionalDetailsMap = null, parentId = 1L, region = mockk(), boundaryRepresentation = null, provincesList = emptyList())
        every { geoLocationJpaMappers.countryToDomainModel(countryEntity, true) } returns countryModel

        val model = ProvinceModel(id = GeoLocationId(0), friendlyId = "bangkok", name = "Bangkok", additionalDetailsMap = null, parentId = countryEntity.id, country = geoLocationJpaMappers.countryToDomainModel(countryEntity, true), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), citiesList = emptyList())
        val provinceEntity = ProvinceEntity(friendlyId = "bangkok", name = "Bangkok", country = countryEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), citiesList = emptyList())
        val savedEntity = ProvinceEntity(friendlyId = "bangkok", name = "Bangkok", country = countryEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val savedModel = ProvinceModel(id = GeoLocationId(2), friendlyId = "bangkok", name = "Bangkok", additionalDetailsMap = null, parentId = countryEntity.id, country = geoLocationJpaMappers.countryToDomainModel(countryEntity, true), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), citiesList = emptyList())

        every { countryJpaRepository.findById(countryId) } returns Optional.of(countryEntity)
        every { provinceJpaRepository.save(any<ProvinceEntity>()) } returns savedEntity
        every { geoLocationJpaMappers.provinceToDomainModel(savedEntity, true) } returns savedModel

        val result = adapter.saveNew(model)

        assertEquals(savedModel, result)
        verify(exactly = 1) { provinceJpaRepository.save(any<ProvinceEntity>()) }
    }

    @Test
    fun `update should update and return ProvinceModel`() {
        val countryId = 1L
        val provinceId = 2L
        val countryEntity = CountryEntity(friendlyId = "thailand", name = "Thailand", region = mockk(), boundaryRepresentation = null, provincesList = emptyList())

        val countryModel = CountryModel(id = GeoLocationId(countryId), friendlyId = "thailand", name = "Thailand", additionalDetailsMap = null, parentId = 1L, region = mockk(), boundaryRepresentation = null, provincesList = emptyList())
        every { geoLocationJpaMappers.countryToDomainModel(countryEntity, true) } returns countryModel

        val model = ProvinceModel(id = GeoLocationId(provinceId), friendlyId = "updated-bangkok", name = "Updated Bangkok", additionalDetailsMap = null, parentId = countryEntity.id, country = geoLocationJpaMappers.countryToDomainModel(countryEntity, true), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), citiesList = emptyList())
        val existingEntity = ProvinceEntity(friendlyId = "bangkok", name = "Bangkok", country = countryEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedEntity = ProvinceEntity(friendlyId = "updated-bangkok", name = "Updated Bangkok", country = countryEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedModel = ProvinceModel(id = GeoLocationId(provinceId), friendlyId = "updated-bangkok", name = "Updated Bangkok", additionalDetailsMap = null, parentId = countryEntity.id, country = geoLocationJpaMappers.countryToDomainModel(countryEntity, true), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))

        every { provinceJpaRepository.findById(provinceId) } returns Optional.of(existingEntity)
        every { countryJpaRepository.findById(countryId) } returns Optional.of(countryEntity)
        every { provinceJpaRepository.save(existingEntity) } returns updatedEntity
        every { geoLocationJpaMappers.provinceToDomainModel(updatedEntity, true) } returns updatedModel

        val result = adapter.update(model)

        assertEquals(updatedModel, result)
        verify(exactly = 1) { provinceJpaRepository.findById(provinceId) }
        verify(exactly = 1) { provinceJpaRepository.save(existingEntity) }
        assertEquals("Updated Bangkok", existingEntity.name)
    }

    @Test
    fun `findById should return ProvinceModel if found`() {
        val countryId = 1L
        val provinceId = 2L
        val countryEntity = CountryEntity(friendlyId = "thailand", name = "Thailand", region = mockk(), boundaryRepresentation = null, provincesList = emptyList())
        val entity = ProvinceEntity(friendlyId = "bangkok", name = "Bangkok", country = countryEntity, boundaryRepresentation = null, citiesList = emptyList())

        val countryModel = CountryModel(id = GeoLocationId(countryId), friendlyId = "thailand", name = "Thailand", additionalDetailsMap = null, parentId = 1L, region = mockk(), boundaryRepresentation = null, provincesList = emptyList())
        every { geoLocationJpaMappers.countryToDomainModel(countryEntity, true) } returns countryModel

        val model = ProvinceModel(id = GeoLocationId(provinceId), friendlyId = "bangkok", name = "Bangkok", additionalDetailsMap = null, parentId = countryEntity.id, country = geoLocationJpaMappers.countryToDomainModel(countryEntity, true), boundaryRepresentation = null, citiesList = emptyList())

        every { provinceJpaRepository.findById(provinceId) } returns Optional.of(entity)
        every { geoLocationJpaMappers.provinceToDomainModel(entity, true) } returns model

        val result = adapter.findById(GeoLocationId(provinceId))

        assertTrue(result.isPresent)
        assertEquals(model, result.get())
    }

    @Test
    fun `findAll should return list of ProvinceModels`() {
        val countryId = 1L
        val countryEntity = CountryEntity(friendlyId = "thailand", name = "Thailand", region = mockk(), boundaryRepresentation = null, provincesList = emptyList())
        val entities = listOf(ProvinceEntity(friendlyId = "bangkok", name = "Bangkok", country = countryEntity, boundaryRepresentation = null, citiesList = emptyList()))

        val countryModel = CountryModel(id = GeoLocationId(countryId), friendlyId = "thailand", name = "Thailand", additionalDetailsMap = null, parentId = 1L, region = mockk(), boundaryRepresentation = null, provincesList = emptyList())
        every { geoLocationJpaMappers.countryToDomainModel(countryEntity, true) } returns countryModel

        val models = listOf(ProvinceModel(id = GeoLocationId(2), friendlyId = "bangkok", name = "Bangkok", additionalDetailsMap = null, parentId = countryEntity.id, country = geoLocationJpaMappers.countryToDomainModel(countryEntity, true), boundaryRepresentation = null, citiesList = emptyList()))

        every { provinceJpaRepository.findAll() } returns entities
        every { geoLocationJpaMappers.provinceToDomainModel(any(), true) } answers { models[0] }

        val result = adapter.findAll()

        assertEquals(models, result)
    }

    @Test
    fun `deleteById should delete Province`() {
        val provinceId = 2L

        every { provinceJpaRepository.deleteById(provinceId) } returns Unit

        adapter.deleteById(GeoLocationId(provinceId))

        verify(exactly = 1) { provinceJpaRepository.deleteById(provinceId) }
    }

    @Test
    fun `findByCountryIdAndNameStartingWith should return filtered list`() {
        val countryId = 1L
        val namePrefix = "B"
        val countryEntity = CountryEntity(friendlyId = "thailand", name = "Thailand", region = mockk(), boundaryRepresentation = null, provincesList = emptyList())
        val entities = listOf(ProvinceEntity(friendlyId = "bangkok", name = "Bangkok", country = countryEntity, boundaryRepresentation = null, citiesList = emptyList()))

        val countryModel = CountryModel(id = GeoLocationId(countryId), friendlyId = "thailand", name = "Thailand", additionalDetailsMap = null, parentId = 1L, region = mockk(), boundaryRepresentation = null, provincesList = emptyList())
        every { geoLocationJpaMappers.countryToDomainModel(countryEntity, true) } returns countryModel

        val models = listOf(ProvinceModel(id = GeoLocationId(2), friendlyId = "bangkok", name = "Bangkok", additionalDetailsMap = null, parentId = countryEntity.id, country = geoLocationJpaMappers.countryToDomainModel(countryEntity, true), boundaryRepresentation = null, citiesList = emptyList()))

        every { provinceJpaRepository.findByCountryIdAndNameStartingWithIgnoreCase(countryId, namePrefix) } returns entities
        every { geoLocationJpaMappers.provinceToDomainModel(any(), true) } answers { models[0] }

        val result = adapter.findByCountryIdAndNameStartingWith(GeoLocationId(countryId), namePrefix)

        assertEquals(models, result)
    }
}
