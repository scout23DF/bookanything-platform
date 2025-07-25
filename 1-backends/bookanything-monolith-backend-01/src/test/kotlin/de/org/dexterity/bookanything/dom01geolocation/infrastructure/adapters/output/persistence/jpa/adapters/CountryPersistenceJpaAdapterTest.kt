package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CountryModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.RegionModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CountryEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.RegionEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CountryJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.RegionJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader
import java.util.*

class CountryPersistenceJpaAdapterTest {

    private val countryJpaRepository: CountryJpaRepository = mockk()
    private val regionJpaRepository: RegionJpaRepository = mockk()
    private val geoLocationJpaMappers: GeoLocationJpaMappers = mockk()
    private val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers = mockk()

    private lateinit var adapter: CountryPersistenceJpaAdapter

    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        adapter = CountryPersistenceJpaAdapter(countryJpaRepository, regionJpaRepository, geoLocationJpaMappers, deepGeoLocationJpaMappers)
    }

    @Test
    fun `saveNew should save and return CountryModel`() {
        val regionId = 1L
        val regionEntity = RegionEntity(friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())

        val regionModel = RegionModel(id = GeoLocationId(regionId), friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, parentId = 1L, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())
        every { geoLocationJpaMappers.regionToDomainModel(regionEntity, true) } returns regionModel

        val model = CountryModel(id = GeoLocationId(0), friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, parentId = regionEntity.id, region = geoLocationJpaMappers.regionToDomainModel(regionEntity, true), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), provincesList = emptyList())
        val countryEntity = CountryEntity(friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, region = regionEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), provincesList = emptyList())
        val savedEntity = CountryEntity(friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, region = regionEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val savedModel = CountryModel(id = GeoLocationId(2), friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, parentId = regionEntity.id, region = geoLocationJpaMappers.regionToDomainModel(regionEntity, true), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), provincesList = emptyList())

        every { regionJpaRepository.findById(regionId) } returns Optional.of(regionEntity)
        every { countryJpaRepository.save(any<CountryEntity>()) } returns savedEntity
        every { geoLocationJpaMappers.countryToDomainModel(savedEntity, true) } returns savedModel

        val result = adapter.saveNew(model)

        assertEquals(savedModel, result)
        verify(exactly = 1) { countryJpaRepository.save(any<CountryEntity>()) }
    }

    @Test
    fun `update should update and return CountryModel`() {
        val regionId = 1L
        val countryId = 2L
        val regionEntity = RegionEntity(friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())

        val regionModel = RegionModel(id = GeoLocationId(regionId), friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, parentId = 1L, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())
        every { geoLocationJpaMappers.regionToDomainModel(regionEntity, true) } returns regionModel

        val model = CountryModel(id = GeoLocationId(countryId), friendlyId = "updated-thailand", name = "Updated Thailand", propertiesDetailsMap = null, parentId = regionEntity.id, region = geoLocationJpaMappers.regionToDomainModel(regionEntity, true), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), provincesList = emptyList())
        val existingEntity = CountryEntity(friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, region = regionEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedEntity = CountryEntity(friendlyId = "updated-thailand", name = "Updated Thailand", propertiesDetailsMap = null, region = regionEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedModel = CountryModel(id = GeoLocationId(countryId), friendlyId = "updated-thailand", name = "Updated Thailand", propertiesDetailsMap = null, parentId = regionEntity.id, region = geoLocationJpaMappers.regionToDomainModel(regionEntity, true), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))

        every { countryJpaRepository.findById(countryId) } returns Optional.of(existingEntity)
        every { regionJpaRepository.findById(regionId) } returns Optional.of(regionEntity)
        every { countryJpaRepository.save(existingEntity) } returns updatedEntity
        every { geoLocationJpaMappers.countryToDomainModel(updatedEntity, true) } returns updatedModel

        val result = adapter.update(model)

        assertEquals(updatedModel, result)
        verify(exactly = 1) { countryJpaRepository.findById(countryId) }
        verify(exactly = 1) { countryJpaRepository.save(existingEntity) }
        assertEquals("Updated Thailand", existingEntity.name)
    }

    @Test
    fun `findById should return CountryModel if found`() {
        val regionId = 1L
        val countryId = 2L
        val regionEntity = RegionEntity(friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())
        val entity = CountryEntity(friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, region = regionEntity, boundaryRepresentation = null, provincesList = emptyList())

        val regionModel = RegionModel(id = GeoLocationId(regionId), friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, parentId = 1L, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())
        every { geoLocationJpaMappers.regionToDomainModel(regionEntity, true) } returns regionModel

        val model = CountryModel(id = GeoLocationId(countryId), friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, parentId = regionEntity.id, region = geoLocationJpaMappers.regionToDomainModel(regionEntity, true), boundaryRepresentation = null, provincesList = emptyList())

        every { countryJpaRepository.findById(countryId) } returns Optional.of(entity)
        every { geoLocationJpaMappers.countryToDomainModel(entity, true) } returns model

        val result = adapter.findById(GeoLocationId(countryId))

        assertTrue(result.isPresent)
        assertEquals(model, result.get())
    }

    @Test
    fun `findAll should return list of CountryModels`() {
        val regionId = 1L
        val regionEntity = RegionEntity(friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())
        val entities = listOf(CountryEntity(friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, region = regionEntity, boundaryRepresentation = null, provincesList = emptyList()))

        val regionModel = RegionModel(id = GeoLocationId(regionId), friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, parentId = 1L, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())
        every { geoLocationJpaMappers.regionToDomainModel(regionEntity, true) } returns regionModel

        val models = listOf(CountryModel(id = GeoLocationId(2), friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, parentId = regionEntity.id, region = geoLocationJpaMappers.regionToDomainModel(regionEntity, true), boundaryRepresentation = null, provincesList = emptyList()))

        every { countryJpaRepository.findAll() } returns entities
        every { geoLocationJpaMappers.countryToDomainModel(any(), true) } answers { models[0] }

        val result = adapter.findAll()

        assertEquals(models, result)
    }

    @Test
    fun `deleteById should delete Country`() {
        val countryId = 2L

        every { countryJpaRepository.deleteById(countryId) } returns Unit

        adapter.deleteById(GeoLocationId(countryId))

        verify(exactly = 1) { countryJpaRepository.deleteById(countryId) }
    }

    @Test
    fun `findByRegionIdAndNameStartingWith should return filtered list`() {
        val regionId = 1L
        val namePrefix = "T"
        val regionEntity = RegionEntity(friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())
        val entities = listOf(CountryEntity(friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, region = regionEntity, boundaryRepresentation = null, provincesList = emptyList()))

        val regionModel = RegionModel(id = GeoLocationId(regionId), friendlyId = "sea", name = "Southeast Asia", propertiesDetailsMap = null, parentId = 1L, continent = mockk(), boundaryRepresentation = null, countriesList = emptyList())
        every { geoLocationJpaMappers.regionToDomainModel(regionEntity, true) } returns regionModel

        val models = listOf(CountryModel(id = GeoLocationId(2), friendlyId = "thailand", name = "Thailand", propertiesDetailsMap = null, parentId = regionEntity.id, region = geoLocationJpaMappers.regionToDomainModel(regionEntity, true), boundaryRepresentation = null, provincesList = emptyList()))

        every { countryJpaRepository.findByRegionIdAndNameStartingWithIgnoreCase(regionId, namePrefix) } returns entities
        every { geoLocationJpaMappers.countryToDomainModel(any(), true) } answers { models[0] }

        val result = adapter.findByRegionIdAndNameStartingWith(GeoLocationId(regionId), namePrefix)

        assertEquals(models, result)
    }
}