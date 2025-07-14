package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CityModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ProvinceModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CityEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ProvinceEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CityJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ProvinceJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.io.WKTReader
import java.util.*

class CityPersistenceJpaAdapterTest {

    private val cityJpaRepository: CityJpaRepository = mockk()
    private val provinceJpaRepository: ProvinceJpaRepository = mockk()
    private val geoLocationJpaMapper: GeoLocationJpaMapper = mockk()

    private lateinit var adapter: CityPersistenceJpaAdapter

    private val wktReader = WKTReader()

    @BeforeEach
    fun setUp() {
        adapter = CityPersistenceJpaAdapter(cityJpaRepository, provinceJpaRepository, geoLocationJpaMapper)
    }

    @Test
    fun `saveNew should save and return CityModel`() {
        val provinceId = 1L
        val provinceEntity = ProvinceEntity(name = "Bangkok", country = mockk(), boundaryRepresentation = null, citiesList = emptyList())

        val provinceModel = ProvinceModel(id = GeoLocationId(provinceId), name = "Bangkok", parentId = 1L, country = mockk(), boundaryRepresentation = null, citiesList = emptyList())
        every { geoLocationJpaMapper.provinceToDomainModel(provinceEntity) } returns provinceModel

        val model = CityModel(id = GeoLocationId(0), name = "Bangkok City", parentId = provinceEntity.id, province = geoLocationJpaMapper.provinceToDomainModel(provinceEntity), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), districtsList = emptyList())
        val cityEntity = CityEntity(name = "Bangkok City", province = provinceEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), districtsList = emptyList())
        val savedEntity = CityEntity(name = "Bangkok City", province = provinceEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val savedModel = CityModel(id = GeoLocationId(2), name = "Bangkok City", parentId = provinceEntity.id, province = geoLocationJpaMapper.provinceToDomainModel(provinceEntity), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), districtsList = emptyList())

        every { provinceJpaRepository.findById(provinceId) } returns Optional.of(provinceEntity)
        every { cityJpaRepository.save(any<CityEntity>()) } returns savedEntity
        every { geoLocationJpaMapper.cityToDomainModel(savedEntity) } returns savedModel

        val result = adapter.saveNew(model)

        assertEquals(savedModel, result)
        verify(exactly = 1) { cityJpaRepository.save(any<CityEntity>()) }
    }

    @Test
    fun `update should update and return CityModel`() {
        val provinceId = 1L
        val cityId = 2L
        val provinceEntity = ProvinceEntity(name = "Bangkok", country = mockk(), boundaryRepresentation = null, citiesList = emptyList())

        val provinceModel = ProvinceModel(id = GeoLocationId(provinceId), name = "Bangkok", parentId = 1L, country = mockk(), boundaryRepresentation = null, citiesList = emptyList())
        every { geoLocationJpaMapper.provinceToDomainModel(provinceEntity) } returns provinceModel

        val model = CityModel(id = GeoLocationId(cityId), name = "Updated Bangkok City", parentId = provinceEntity.id, province = geoLocationJpaMapper.provinceToDomainModel(provinceEntity), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"), districtsList = emptyList())
        val existingEntity = CityEntity(name = "Bangkok City", province = provinceEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedEntity = CityEntity(name = "Updated Bangkok City", province = provinceEntity, boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))
        val updatedModel = CityModel(id = GeoLocationId(cityId), name = "Updated Bangkok City", parentId = provinceEntity.id, province = geoLocationJpaMapper.provinceToDomainModel(provinceEntity), boundaryRepresentation = wktReader.read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"))

        every { cityJpaRepository.findById(cityId) } returns Optional.of(existingEntity)
        every { provinceJpaRepository.findById(provinceId) } returns Optional.of(provinceEntity)
        every { cityJpaRepository.save(existingEntity) } returns updatedEntity
        every { geoLocationJpaMapper.cityToDomainModel(updatedEntity) } returns updatedModel

        val result = adapter.update(model)

        assertEquals(updatedModel, result)
        verify(exactly = 1) { cityJpaRepository.findById(cityId) }
        verify(exactly = 1) { cityJpaRepository.save(existingEntity) }
        assertEquals("Updated Bangkok City", existingEntity.name)
    }

    @Test
    fun `findById should return CityModel if found`() {
        val provinceId = 1L
        val cityId = 2L
        val provinceEntity = ProvinceEntity(name = "Bangkok", country = mockk(), boundaryRepresentation = null, citiesList = emptyList())
        val entity = CityEntity(name = "Bangkok City", province = provinceEntity, boundaryRepresentation = null, districtsList = emptyList())

        val provinceModel = ProvinceModel(id = GeoLocationId(provinceId), name = "Bangkok", parentId = 1L, country = mockk(), boundaryRepresentation = null, citiesList = emptyList())
        every { geoLocationJpaMapper.provinceToDomainModel(provinceEntity) } returns provinceModel

        val model = CityModel(id = GeoLocationId(cityId), name = "Bangkok City", parentId = provinceEntity.id, province = geoLocationJpaMapper.provinceToDomainModel(provinceEntity), boundaryRepresentation = null, districtsList = emptyList())

        every { cityJpaRepository.findById(cityId) } returns Optional.of(entity)
        every { geoLocationJpaMapper.cityToDomainModel(entity) } returns model

        val result = adapter.findById(GeoLocationId(cityId))

        assertTrue(result.isPresent)
        assertEquals(model, result.get())
    }

    @Test
    fun `findAll should return list of CityModels`() {
        val provinceId = 1L
        val provinceEntity = ProvinceEntity(name = "Bangkok", country = mockk(), boundaryRepresentation = null, citiesList = emptyList())
        val entities = listOf(CityEntity(name = "Bangkok City", province = provinceEntity, boundaryRepresentation = null, districtsList = emptyList()))

        val provinceModel = ProvinceModel(id = GeoLocationId(provinceId), name = "Bangkok", parentId = 1L, country = mockk(), boundaryRepresentation = null, citiesList = emptyList())
        every { geoLocationJpaMapper.provinceToDomainModel(provinceEntity) } returns provinceModel

        val models = listOf(CityModel(id = GeoLocationId(2), name = "Bangkok City", parentId = provinceEntity.id, province = geoLocationJpaMapper.provinceToDomainModel(provinceEntity), boundaryRepresentation = null, districtsList = emptyList()))

        every { cityJpaRepository.findAll() } returns entities
        every { geoLocationJpaMapper.cityToDomainModel(any()) } answers { models[0] }

        val result = adapter.findAll()

        assertEquals(models, result)
    }

    @Test
    fun `deleteById should delete City`() {
        val cityId = 2L

        every { cityJpaRepository.deleteById(cityId) } returns Unit

        adapter.deleteById(GeoLocationId(cityId))

        verify(exactly = 1) { cityJpaRepository.deleteById(cityId) }
    }

    @Test
    fun `findByProvinceIdAndNameStartingWith should return filtered list`() {
        val provinceId = 1L
        val namePrefix = "B"
        val provinceEntity = ProvinceEntity(name = "Bangkok", country = mockk(), boundaryRepresentation = null, citiesList = emptyList())
        val entities = listOf(CityEntity(name = "Bangkok City", province = provinceEntity, boundaryRepresentation = null, districtsList = emptyList()))

        val provinceModel = ProvinceModel(id = GeoLocationId(provinceId), name = "Bangkok", parentId = 1L, country = mockk(), boundaryRepresentation = null, citiesList = emptyList())
        every { geoLocationJpaMapper.provinceToDomainModel(provinceEntity) } returns provinceModel

        val models = listOf(CityModel(id = GeoLocationId(2), name = "Bangkok City", parentId = provinceEntity.id, province = geoLocationJpaMapper.provinceToDomainModel(provinceEntity), boundaryRepresentation = null, districtsList = emptyList()))

        every { cityJpaRepository.findByProvinceIdAndNameStartingWithIgnoreCase(provinceId, namePrefix) } returns entities
        every { geoLocationJpaMapper.cityToDomainModel(any()) } answers { models[0] }

        val result = adapter.findByProvinceIdAndNameStartingWith(GeoLocationId(provinceId), namePrefix)

        assertEquals(models, result)
    }
}