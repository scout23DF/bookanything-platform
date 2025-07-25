package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.AddressEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.DistrictEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.AddressJpaMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.AddressJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.DistrictJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.util.*

class AddressPersistenceJpaAdapterTest {

    private val addressJpaRepository: AddressJpaRepository = mockk()
    private val districtJpaRepository: DistrictJpaRepository = mockk()
    private val addressJpaMapper: AddressJpaMapper = mockk()
    private val geoLocationJpaMappers: GeoLocationJpaMappers = mockk()

    private lateinit var adapter: AddressPersistenceJpaAdapter

    private val geometryFactory = GeometryFactory()

    @BeforeEach
    fun setUp() {
        adapter = AddressPersistenceJpaAdapter(addressJpaRepository, districtJpaRepository, addressJpaMapper)
    }

    @Test
    fun `saveNew should save and return AddressModel`() {
        val districtId = 1L
        val districtEntity = DistrictEntity(friendlyId = "downtown", name = "Downtown", city = mockk(), boundaryRepresentation = null, addressesList = emptyList())

        val districtModel = DistrictModel(id = GeoLocationId(districtId), friendlyId = "downtown", name = "Downtown", additionalDetailsMap = null, parentId = 1L, city = mockk(), boundaryRepresentation = null, addressesList = emptyList())
        every { geoLocationJpaMappers.districtToDomainModel(districtEntity, false) } returns districtModel

        val model = AddressModel(
            id = GeoLocationId(0),
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            district = geoLocationJpaMappers.districtToDomainModel(districtEntity, false),
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = GeoCoordinate(1.0, 2.0),
            status = StatusType.ACTIVE
        )
        val addressEntity = AddressEntity(
            id = null,
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            district = districtEntity,
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = geometryFactory.createPoint(org.locationtech.jts.geom.Coordinate(2.0, 1.0)),
            status = StatusType.ACTIVE
        )
        val savedEntity = AddressEntity(
            id = 1,
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            district = districtEntity,
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = geometryFactory.createPoint(org.locationtech.jts.geom.Coordinate(2.0, 1.0)),
            status = StatusType.ACTIVE
        )
        val savedModel = AddressModel(
            id = GeoLocationId(1),
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            district = geoLocationJpaMappers.districtToDomainModel(districtEntity, false),
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = GeoCoordinate(1.0, 2.0),
            status = StatusType.ACTIVE
        )

        every { districtJpaRepository.findById(any()) } returns Optional.of(districtEntity)
        every { addressJpaMapper.buildPointFromGeoCoordinate(any()) } returns geometryFactory.createPoint(Coordinate(1.0, 2.0))
        every { addressJpaMapper.addressToJpaEntity(model) } returns addressEntity
        every { addressJpaRepository.save(any()) } returns savedEntity
        every { addressJpaMapper.addressToDomainModel(savedEntity) } returns savedModel

        val result = adapter.saveNew(model)

        assertEquals(savedModel, result)
        verify(exactly = 1) { addressJpaRepository.save(any()) }
    }

    @Test
    fun `update should update and return AddressModel`() {
        val districtId = 1L
        val addressId = 1L
        val fakePoint : Point = geometryFactory.createPoint(Coordinate(2.0, 1.0))

        val districtEntity = DistrictEntity(friendlyId = "downtown", name = "Downtown", city = mockk(), boundaryRepresentation = null, addressesList = emptyList())

        val districtModel = DistrictModel(id = GeoLocationId(districtId), friendlyId = "downtown", name = "Downtown", additionalDetailsMap = null, parentId = 1L, city = mockk(), boundaryRepresentation = null, addressesList = emptyList())
        every { geoLocationJpaMappers.districtToDomainModel(districtEntity, false) } returns districtModel

        val model = AddressModel(
            id = GeoLocationId(addressId),
            streetName = "Updated St",
            houseNumber = "456",
            floorNumber = "2",
            doorNumber = "B",
            addressLine2 = "Suite 2",
            postalCode = "54321",
            district = geoLocationJpaMappers.districtToDomainModel(districtEntity, false),
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = GeoCoordinate(3.0, 4.0),
            status = StatusType.ACTIVE
        )
        val existingEntity = AddressEntity(
            id = addressId,
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            district = districtEntity,
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = geometryFactory.createPoint(Coordinate(2.0, 1.0)),
            status = StatusType.ACTIVE
        )
        val updatedEntity = AddressEntity(
            id = addressId,
            streetName = "Updated St",
            houseNumber = "456",
            floorNumber = "2",
            doorNumber = "B",
            addressLine2 = "Suite 2",
            postalCode = "54321",
            district = districtEntity,
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = geometryFactory.createPoint(Coordinate(4.0, 3.0)),
            status = StatusType.ACTIVE
        )
        val updatedModel = AddressModel(
            id = GeoLocationId(addressId),
            streetName = "Updated St",
            houseNumber = "456",
            floorNumber = "2",
            doorNumber = "B",
            addressLine2 = "Suite 2",
            postalCode = "54321",
            district = geoLocationJpaMappers.districtToDomainModel(districtEntity, false),
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = GeoCoordinate(3.0, 4.0),
            status = StatusType.ACTIVE
        )

        every { addressJpaRepository.findById(addressId) } returns Optional.of(existingEntity)
        every { districtJpaRepository.findById(districtId) } returns Optional.of(districtEntity)
        every { addressJpaRepository.save(existingEntity) } returns updatedEntity
        every { addressJpaMapper.buildPointFromGeoCoordinate(any()) } returns fakePoint
        every { addressJpaMapper.addressToDomainModel(updatedEntity) } returns updatedModel

        val result = adapter.update(model)

        assertEquals(updatedModel, result)
        verify(exactly = 1) { addressJpaRepository.findById(addressId) }
        verify(exactly = 1) { addressJpaRepository.save(existingEntity) }
        assertEquals("Updated St", existingEntity.streetName)
    }

    @Test
    fun `findById should return AddressModel if found`() {
        val addressId = 1L
        val districtEntity = DistrictEntity(friendlyId = "downtown", name = "Downtown", city = mockk(), boundaryRepresentation = null, addressesList = emptyList())

        val districtModel = DistrictModel(id = GeoLocationId(1L), friendlyId = "downtown", name = "Downtown", additionalDetailsMap = null, parentId = 1L, city = mockk(), boundaryRepresentation = null, addressesList = emptyList())
        every { geoLocationJpaMappers.districtToDomainModel(districtEntity, false) } returns districtModel

        val entity = AddressEntity(streetName = "Main St", houseNumber = "123", postalCode = "12345", district = districtEntity, districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country", floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null, status = null)
        val model = AddressModel(id = GeoLocationId(addressId), streetName = "Main St", houseNumber = "123", postalCode = "12345", district = geoLocationJpaMappers.districtToDomainModel(districtEntity, false), districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country", floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null, status = null)

        every { addressJpaRepository.findById(addressId) } returns Optional.of(entity)
        every { addressJpaMapper.addressToDomainModel(entity) } returns model

        val result = adapter.findById(GeoLocationId(addressId))

        assertTrue(result.isPresent)
        assertEquals(model, result.get())
    }

    @Test
    fun `findAll should return list of AddressModels`() {
        val districtEntity = DistrictEntity(friendlyId = "downtown", name = "Downtown", city = mockk(), boundaryRepresentation = null, addressesList = emptyList())

        val districtModel = DistrictModel(id = GeoLocationId(1L), friendlyId = "downtown", name = "Downtown", additionalDetailsMap = null, parentId = 1L, city = mockk(), boundaryRepresentation = null, addressesList = emptyList())
        every { geoLocationJpaMappers.districtToDomainModel(districtEntity, false) } returns districtModel

        val entities = listOf(AddressEntity(streetName = "Main St", houseNumber = "123", postalCode = "12345", district = districtEntity, districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country", floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null, status = null))
        val models = listOf(AddressModel(id = GeoLocationId(1), streetName = "Main St", houseNumber = "123", postalCode = "12345", district = geoLocationJpaMappers.districtToDomainModel(districtEntity, false), districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country", floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null, status = null))

        every { addressJpaRepository.findAll() } returns entities
        every { addressJpaMapper.addressToDomainModel(any()) } answers { models[0] }

        val result = adapter.findAll()

        assertEquals(models, result)
    }

    @Test
    fun `deleteById should delete Address`() {
        val addressId = 1L

        every { addressJpaRepository.deleteById(addressId) } returns Unit

        adapter.deleteById(GeoLocationId(addressId))

        verify(exactly = 1) { addressJpaRepository.deleteById(addressId) }
    }

    @Test
    fun `findByDistrictIdAndStreetNameStartingWith should return filtered list`() {
        val districtId = 1L
        val namePrefix = "Main"
        val districtEntity = DistrictEntity(friendlyId = "downtown", name = "Downtown", city = mockk(), boundaryRepresentation = null, addressesList = emptyList())
        val entities = listOf(AddressEntity(id = 1, streetName = "Main St", houseNumber = "123", postalCode = "12345", district = districtEntity, districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country", floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null, status = null))

        val districtModel = DistrictModel(id = GeoLocationId(districtId), friendlyId = "downtown", name = "Downtown", additionalDetailsMap = null, parentId = 1L, city = mockk(), boundaryRepresentation = null, addressesList = emptyList())
        every { geoLocationJpaMappers.districtToDomainModel(districtEntity, false) } returns districtModel

        val models = listOf(AddressModel(id = GeoLocationId(1), streetName = "Main St", houseNumber = "123", postalCode = "12345", district = geoLocationJpaMappers.districtToDomainModel(districtEntity, false), districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country", floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null, status = null))

        every { addressJpaRepository.findByDistrictIdAndStreetNameStartingWithIgnoreCase(districtId, namePrefix) } returns entities
        every { addressJpaMapper.addressToDomainModel(any()) } answers { models[0] }

        val result = adapter.findByDistrictIdAndStreetNameStartingWith(GeoLocationId(districtId), namePrefix)

        assertEquals(models, result)
    }
}
