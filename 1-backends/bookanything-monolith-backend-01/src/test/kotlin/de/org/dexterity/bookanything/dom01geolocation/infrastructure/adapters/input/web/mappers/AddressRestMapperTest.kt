package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.AddressResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateAddressRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateAddressRequest
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AddressRestMapperTest {

    private lateinit var mapper: AddressRestMapper

    @BeforeEach
    fun setUp() {
        mapper = AddressRestMapper()
    }

    @Test
    fun `fromAddressModelToResponse should map AddressModel correctly`() {
        val districtModel = DistrictModel(id = GeoLocationId(1), friendlyId = "downtown", name = "Downtown", parentId = 1L, city = mockk(), boundaryRepresentation = null, addressesList = emptyList())
        val model = AddressModel(
            id = GeoLocationId(100),
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = GeoCoordinate(1.0, 2.0),
            status = StatusType.ACTIVE,
            district = districtModel
        )
        val response = mapper.fromAddressModelToResponse(model)

        assertEquals(model.id.id, response.id)
        assertEquals(model.streetName, response.streetName)
        assertEquals(model.houseNumber, response.houseNumber)
        assertEquals(model.floorNumber, response.floorNumber)
        assertEquals(model.doorNumber, response.doorNumber)
        assertEquals(model.addressLine2, response.addressLine2)
        assertEquals(model.postalCode, response.postalCode)
        assertEquals(model.districtName, response.districtName)
        assertEquals(model.cityName, response.cityName)
        assertEquals(model.provinceName, response.provinceName)
        assertEquals(model.countryName, response.countryName)
    }

    @Test
    fun `fromCreateAddressRequestToAddressModel should map CreateAddressRequest correctly`() {

        val continentModel = ContinentModel(id = GeoLocationId(1), friendlyId = "europe", name = "Europe", parentId = null)
        val regionModel = RegionModel(id = GeoLocationId(2), friendlyId = "eu-central", name = "EU Central", additionalDetailsMap = null, parentId = 1L, continent = continentModel)
        val countryModel = CountryModel(id = GeoLocationId(3), friendlyId = "germany", name = "Germany", parentId = 2L, region = regionModel)
        val provinceModel = ProvinceModel(id = GeoLocationId(4), friendlyId = "berlin-province", name = "Berlin", parentId = 3L, country = countryModel)
        val cityModel = CityModel(id = GeoLocationId(5), friendlyId = "berlin-city", name = "Berlin", parentId = 4L, province = provinceModel)
        val districtModel = DistrictModel(id = GeoLocationId(6), friendlyId = "downtown", name = "Downtown", parentId = 5L, city = cityModel, boundaryRepresentation = null, addressesList = emptyList())

        val request = CreateAddressRequest(
            streetName = "New St",
            houseNumber = "456",
            floorNumber = "2",
            doorNumber = "B",
            addressLine2 = "Suite 2",
            postalCode = "67890",
            districtId = 6,
            latitude = 3.0,
            longitude = 4.0,
            status = "ACTIVE"
        )
        val model = mapper.fromCreateAddressRequestToAddressModel(request, districtModel)

        assertEquals(request.streetName, model.streetName)
        assertEquals(request.houseNumber, model.houseNumber)
        assertEquals(request.floorNumber, model.floorNumber)
        assertEquals(request.doorNumber, model.doorNumber)
        assertEquals(request.addressLine2, model.addressLine2)
        assertEquals(request.postalCode, model.postalCode)
        assertEquals(request.latitude, model.coordinates?.latitude)
        assertEquals(request.longitude, model.coordinates?.longitude)
        assertEquals(StatusType.ACTIVE, model.status)
        assertEquals(districtModel, model.district)
    }

    @Test
    fun `fromUpdateAddressRequestToAddressModel should map UpdateAddressRequest correctly`() {
        val existingAddress = AddressModel(
            id = GeoLocationId(100),
            streetName = "Old St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            district = mockk(),
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = GeoCoordinate(1.0, 2.0),
            status = StatusType.ACTIVE
        )
        val updateRequest = UpdateAddressRequest(
            streetName = "Updated St",
            houseNumber = "789",
            floorNumber = "3",
            doorNumber = "C",
            addressLine2 = "Unit 3",
            postalCode = "54321"
        )
        val updatedModel = mapper.fromUpdateAddressRequestToAddressModel(updateRequest, existingAddress)

        assertEquals(updateRequest.streetName, updatedModel.streetName)
        assertEquals(updateRequest.houseNumber, updatedModel.houseNumber)
        assertEquals(updateRequest.floorNumber, updatedModel.floorNumber)
        assertEquals(updateRequest.doorNumber, updatedModel.doorNumber)
        assertEquals(updateRequest.addressLine2, updatedModel.addressLine2)
        assertEquals(updateRequest.postalCode, updatedModel.postalCode)
        assertEquals(existingAddress.id, updatedModel.id)
        assertEquals(existingAddress.district, updatedModel.district)
    }
}