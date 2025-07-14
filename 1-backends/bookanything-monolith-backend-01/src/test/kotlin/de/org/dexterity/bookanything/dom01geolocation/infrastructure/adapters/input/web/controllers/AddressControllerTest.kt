package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.AddressUseCase
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.DistrictUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.AddressResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateAddressRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateAddressRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.AddressRestMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.*

class AddressControllerTest {

    private val addressUseCase: AddressUseCase = mockk()
    private val districtUseCase: DistrictUseCase = mockk()
    private val addressRestMapper: AddressRestMapper = mockk()

    private lateinit var controller: AddressController

    @BeforeEach
    fun setUp() {
        controller = AddressController(addressUseCase, districtUseCase, addressRestMapper)
    }

    @Test
    fun `createAddress should return created address`() {
        val districtId = 1L
        val createRequest = CreateAddressRequest(
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            districtId = districtId,
            latitude = 1.0,
            longitude = 2.0,
            status = "ACTIVE"
        )
        val districtModel = DistrictModel(id = GeoLocationId(districtId), name = "Downtown", parentId = 1L, city = mockk(), boundaryRepresentation = null, addressesList = emptyList())
        val addressModel = AddressModel(
            id = GeoLocationId(1),
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            district = districtModel,
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country",
            coordinates = GeoCoordinate(1.0, 2.0),
            status = StatusType.ACTIVE
        )
        val addressResponse = AddressResponse(
            id = 1,
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country"
        )

        every { districtUseCase.findById(GeoLocationId(districtId)) } returns Optional.of(districtModel)
        every { addressRestMapper.fromCreateAddressRequestToAddressModel(createRequest, districtModel) } returns addressModel
        every { addressUseCase.create(addressModel) } returns addressModel
        every { addressRestMapper.fromAddressModelToResponse(addressModel) } returns addressResponse

        val response = controller.createAddress(createRequest)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(addressResponse, response.body)
        verify(exactly = 1) { addressUseCase.create(addressModel) }
    }

    @Test
    fun `getAddress should return address if found`() {
        val addressId = 1L
        val addressModel = AddressModel(
            id = GeoLocationId(addressId),
            streetName = "Main St",
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
        val addressResponse = AddressResponse(
            id = addressId,
            streetName = "Main St",
            houseNumber = "123",
            floorNumber = "1",
            doorNumber = "A",
            addressLine2 = "Apt 1",
            postalCode = "12345",
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country"
        )

        every { addressUseCase.findById(GeoLocationId(addressId)) } returns Optional.of(addressModel)
        every { addressRestMapper.fromAddressModelToResponse(addressModel) } returns addressResponse

        val response = controller.getAddress(addressId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(addressResponse, response.body)
        verify(exactly = 1) { addressUseCase.findById(GeoLocationId(addressId)) }
    }

    @Test
    fun `getAllAddresses should return list of addresses`() {
        val addressList = listOf(
            AddressModel(id = GeoLocationId(1), streetName = "Main St", houseNumber = "123", floorNumber = "1", doorNumber = "A", addressLine2 = "Apt 1", postalCode = "12345", district = mockk(), districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country", coordinates = GeoCoordinate(1.0, 2.0), status = StatusType.ACTIVE),
            AddressModel(id = GeoLocationId(2), streetName = "Second St", houseNumber = "456", floorNumber = "2", doorNumber = "B", addressLine2 = "Suite 2", postalCode = "67890", district = mockk(), districtName = "Uptown", cityName = "City", provinceName = "Province", countryName = "Country", coordinates = GeoCoordinate(3.0, 4.0), status = StatusType.ACTIVE)
        )
        val addressResponseList = listOf(
            AddressResponse(id = 1, streetName = "Main St", houseNumber = "123", floorNumber = "1", doorNumber = "A", addressLine2 = "Apt 1", postalCode = "12345", districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country"),
            AddressResponse(id = 2, streetName = "Second St", houseNumber = "456", floorNumber = "2", doorNumber = "B", addressLine2 = "Suite 2", postalCode = "67890", districtName = "Uptown", cityName = "City", provinceName = "Province", countryName = "Country")
        )

        every { addressUseCase.findAll() } returns addressList
        every { addressRestMapper.fromAddressModelToResponse(any()) } answers { callOriginal() }

        val result = controller.getAllAddresses()

        assertEquals(2, result.size)
        assertEquals(addressResponseList, result)
        verify(exactly = 1) { addressUseCase.findAll() }
    }

    @Test
    fun `updateAddress should return updated address`() {
        val addressId = 1L
        val updateRequest = UpdateAddressRequest(
            streetName = "Updated St",
            houseNumber = "789",
            floorNumber = "3",
            doorNumber = "C",
            addressLine2 = "Unit 3",
            postalCode = "54321"
        )
        val existingAddressModel = AddressModel(
            id = GeoLocationId(addressId),
            streetName = "Main St",
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
        val updatedAddressModel = existingAddressModel.copy(
            streetName = updateRequest.streetName,
            houseNumber = updateRequest.houseNumber,
            floorNumber = updateRequest.floorNumber,
            doorNumber = updateRequest.doorNumber,
            addressLine2 = updateRequest.addressLine2,
            postalCode = updateRequest.postalCode
        )
        val updatedAddressResponse = AddressResponse(
            id = addressId,
            streetName = "Updated St",
            houseNumber = "789",
            floorNumber = "3",
            doorNumber = "C",
            addressLine2 = "Unit 3",
            postalCode = "54321",
            districtName = "Downtown",
            cityName = "City",
            provinceName = "Province",
            countryName = "Country"
        )

        every { addressUseCase.findById(GeoLocationId(addressId)) } returns Optional.of(existingAddressModel)
        every { addressRestMapper.fromUpdateAddressRequestToAddressModel(updateRequest, existingAddressModel) } returns updatedAddressModel
        every { addressUseCase.update(updatedAddressModel) } returns updatedAddressModel
        every { addressRestMapper.fromAddressModelToResponse(updatedAddressModel) } returns updatedAddressResponse

        val response = controller.updateAddress(addressId, updateRequest)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(updatedAddressResponse, response.body)
        verify(exactly = 1) { addressUseCase.update(updatedAddressModel) }
    }

    @Test
    fun `deleteAddress should return no content`() {
        val addressId = 1L

        every { addressUseCase.deleteById(GeoLocationId(addressId)) } returns Unit

        val response = controller.deleteAddress(addressId)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify(exactly = 1) { addressUseCase.deleteById(GeoLocationId(addressId)) }
    }

    @Test
    fun `searchAddresses should return list of addresses`() {
        val districtId = 1L
        val streetNamePrefix = "Main"
        val addressList = listOf(
            AddressModel(id = GeoLocationId(1), streetName = "Main St", houseNumber = "123", floorNumber = "1", doorNumber = "A", addressLine2 = "Apt 1", postalCode = "12345", district = mockk(), districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country", coordinates = GeoCoordinate(1.0, 2.0), status = StatusType.ACTIVE),
            AddressModel(id = GeoLocationId(2), streetName = "Main Ave", houseNumber = "456", floorNumber = "2", doorNumber = "B", addressLine2 = "Suite 2", postalCode = "67890", district = mockk(), districtName = "Uptown", cityName = "City", provinceName = "Province", countryName = "Country", coordinates = GeoCoordinate(3.0, 4.0), status = StatusType.ACTIVE)
        )
        val addressResponseList = listOf(
            AddressResponse(id = 1, streetName = "Main St", houseNumber = "123", floorNumber = "1", doorNumber = "A", addressLine2 = "Apt 1", postalCode = "12345", districtName = "Downtown", cityName = "City", provinceName = "Province", countryName = "Country"),
            AddressResponse(id = 2, streetName = "Main Ave", houseNumber = "456", floorNumber = "2", doorNumber = "B", addressLine2 = "Suite 2", postalCode = "67890", districtName = "Uptown", cityName = "City", provinceName = "Province", countryName = "Country")
        )

        every { addressUseCase.findByDistrictIdAndStreetNameStartingWith(GeoLocationId(districtId), streetNamePrefix) } returns addressList
        every { addressRestMapper.fromAddressModelToResponse(any()) } answers { callOriginal() }

        val result = controller.searchAddresses(districtId, streetNamePrefix)

        assertEquals(2, result.size)
        assertEquals(addressResponseList, result)
        verify(exactly = 1) { addressUseCase.findByDistrictIdAndStreetNameStartingWith(GeoLocationId(districtId), streetNamePrefix) }
    }
}