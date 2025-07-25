package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.AddressResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateAddressRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateAddressRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.*
import de.org.dexterity.bookanything.shared.integrationtests.AbstractIntegrationTest
import de.org.dexterity.bookanything.shared.integrationtests.util.TestDatabaseCleaner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AddressControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var addressJpaRepository: AddressJpaRepository

    @Autowired
    private lateinit var districtJpaRepository: DistrictJpaRepository

    @Autowired
    private lateinit var cityJpaRepository: CityJpaRepository

    @Autowired
    private lateinit var provinceJpaRepository: ProvinceJpaRepository

    @Autowired
    private lateinit var countryJpaRepository: CountryJpaRepository

    @Autowired
    private lateinit var regionJpaRepository: RegionJpaRepository

    @Autowired
    private lateinit var continentJpaRepository: ContinentJpaRepository

    @Autowired
    private lateinit var testDatabaseCleaner: TestDatabaseCleaner

    private lateinit var testDistrict: DistrictEntity

    @BeforeEach
    fun setUp() {
        testDatabaseCleaner.cleanAndResetDatabase()

        val continent = continentJpaRepository.save(ContinentEntity(friendlyId = "south-america", name = "South America", boundaryRepresentation = null))
        val region = regionJpaRepository.save(RegionEntity(friendlyId = "south-america-region", name = "South America", continent = continent, boundaryRepresentation = null))
        val country = countryJpaRepository.save(CountryEntity(friendlyId = "brazil", name = "Brazil", region = region, boundaryRepresentation = null))
        val province = provinceJpaRepository.save(ProvinceEntity(friendlyId = "sao-paulo-province", name = "São Paulo", country = country, boundaryRepresentation = null))
        val city = cityJpaRepository.save(CityEntity(friendlyId = "sao-paulo-city", name = "São Paulo", province = province, boundaryRepresentation = null))
        testDistrict = districtJpaRepository.save(DistrictEntity(friendlyId = "vila-madalena", name = "Vila Madalena", city = city, boundaryRepresentation = null))
    }

    @AfterEach
    fun tearDown() {

        testDatabaseCleaner.cleanAndResetDatabase()
    }

    @Test
    fun `should create address successfully`() {
        val createRequest = CreateAddressRequest(
            streetName = "Rua Girassol",
            houseNumber = "123",
            floorNumber = "4",
            doorNumber = "5",
            addressLine2 = "Apt 45",
            postalCode = "05433-000",
            districtId = testDistrict.id!!,
            latitude = -23.55,
            longitude = -46.63,
            status = "ACTIVE"
        )

        val result = mockMvc.post("/api/v1/addresses") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
            with(jwt())
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val response = objectMapper.readValue<AddressResponse>(result.response.contentAsString)
        assertNotNull(response.id)
        assertEquals(createRequest.streetName, response.streetName)

        val savedAddress = addressJpaRepository.findById(response.id!!)
        assertTrue(savedAddress.isPresent)
        assertEquals(createRequest.streetName, savedAddress.get().streetName)
    }

    @Test
    fun `should return bad request when creating address with non-existent district`() {
        val createRequest = CreateAddressRequest(
            streetName = "Rua dos Pinheiros",
            houseNumber = "456",
            floorNumber = null,
            doorNumber = null,
            addressLine2 = null,
            postalCode = "05422-000",
            districtId = 999L, // Non-existent ID
            latitude = -23.55,
            longitude = -46.63,
            status = "ACTIVE"
        )

        mockMvc.post("/api/v1/addresses") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
            with(jwt())
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return address when found by id`() {
        val address = addressJpaRepository.save(
            AddressEntity(
                streetName = "Rua Harmonia",
                houseNumber = "789",
                postalCode = "05435-000",
                district = testDistrict,
                districtName = testDistrict.name,
                cityName = testDistrict.city.name,
                provinceName = testDistrict.city.province.name,
                countryName = testDistrict.city.province.country.name,
                floorNumber = null,
                doorNumber = null,
                addressLine2 = null,
                coordinates = null
            )
        )

        mockMvc.get("/api/v1/addresses/{id}", address.id) {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(address.id) }
            jsonPath("$.streetName") { value("Rua Harmonia") }
        }
    }

    @Test
    fun `should return not found when address by id does not exist`() {
        mockMvc.get("/api/v1/addresses/{id}", 999L) {
            with(jwt())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return all addresses`() {
        addressJpaRepository.save(AddressEntity(streetName = "Rua Fidalga", houseNumber = "1", postalCode = "111", district = testDistrict, districtName = testDistrict.name, cityName = testDistrict.city.name, provinceName = testDistrict.city.province.name, countryName = testDistrict.city.province.country.name, floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null))
        addressJpaRepository.save(AddressEntity(streetName = "Rua Inácio Pereira da Rocha", houseNumber = "2", postalCode = "222", district = testDistrict, districtName = testDistrict.name, cityName = testDistrict.city.name, provinceName = testDistrict.city.province.name, countryName = testDistrict.city.province.country.name, floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null))

        mockMvc.get("/api/v1/addresses") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }
    }

    @Test
    fun `should update address successfully`() {
        val address = addressJpaRepository.save(
            AddressEntity(
                streetName = "Rua Original",
                houseNumber = "100",
                postalCode = "10000-000",
                district = testDistrict,
                districtName = testDistrict.name,
                cityName = testDistrict.city.name,
                provinceName = testDistrict.city.province.name,
                countryName = testDistrict.city.province.country.name,
                floorNumber = null,
                doorNumber = null,
                addressLine2 = null,
                coordinates = null
            )
        )
        val updateRequest = UpdateAddressRequest(
            streetName = "Rua Atualizada",
            houseNumber = "200",
            floorNumber = "1",
            doorNumber = "2",
            addressLine2 = "Casa 2",
            postalCode = "20000-000"
        )

        mockMvc.put("/api/v1/addresses/{id}", address.id) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.streetName") { value("Rua Atualizada") }
            jsonPath("$.postalCode") { value("20000-000") }
        }

        val updatedAddress = addressJpaRepository.findById(address.id!!).get()
        assertEquals("Rua Atualizada", updatedAddress.streetName)
        assertEquals("20000-000", updatedAddress.postalCode)
    }

    @Test
    fun `should return not found when updating non-existent address`() {
        val updateRequest = UpdateAddressRequest(streetName = "Doesn't Matter", houseNumber = "1", floorNumber = null, doorNumber = null, addressLine2 = null, postalCode = "1")

        mockMvc.put("/api/v1/addresses/{id}", 999L) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
            with(jwt())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should delete address successfully`() {
        val address = addressJpaRepository.save(
            AddressEntity(
                streetName = "Rua a ser Deletada",
                houseNumber = "123",
                postalCode = "12345-678",
                district = testDistrict,
                districtName = testDistrict.name,
                cityName = testDistrict.city.name,
                provinceName = testDistrict.city.province.name,
                countryName = testDistrict.city.province.country.name,
                floorNumber = null,
                doorNumber = null,
                addressLine2 = null,
                coordinates = null
            )
        )
        assertTrue(addressJpaRepository.findById(address.id!!).isPresent)

        mockMvc.delete("/api/v1/addresses/{id}", address.id) {
            with(jwt())
        }.andExpect {
            status { isNoContent() }
        }

        assertFalse(addressJpaRepository.findById(address.id!!).isPresent)
    }

    @Test
    fun `should return addresses matching search criteria`() {
        addressJpaRepository.save(AddressEntity(streetName = "Rua das Flores", houseNumber = "1", postalCode = "1", district = testDistrict, districtName = testDistrict.name, cityName = testDistrict.city.name, provinceName = testDistrict.city.province.name, countryName = testDistrict.city.province.country.name, floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null))
        addressJpaRepository.save(AddressEntity(streetName = "Rua das Palmeiras", houseNumber = "2", postalCode = "2", district = testDistrict, districtName = testDistrict.name, cityName = testDistrict.city.name, provinceName = testDistrict.city.province.name, countryName = testDistrict.city.province.country.name, floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null))
        addressJpaRepository.save(AddressEntity(streetName = "Avenida Paulista", houseNumber = "3", postalCode = "3", district = testDistrict, districtName = testDistrict.name, cityName = testDistrict.city.name, provinceName = testDistrict.city.province.name, countryName = testDistrict.city.province.country.name, floorNumber = null, doorNumber = null, addressLine2 = null, coordinates = null))

        mockMvc.get("/api/v1/addresses/search") {
            param("districtId", testDistrict.id!!.toString())
            param("streetNamePrefix", "Rua das")
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].streetName") { value("Rua das Flores") }
            jsonPath("$[1].streetName") { value("Rua das Palmeiras") }
        }
    }
}
