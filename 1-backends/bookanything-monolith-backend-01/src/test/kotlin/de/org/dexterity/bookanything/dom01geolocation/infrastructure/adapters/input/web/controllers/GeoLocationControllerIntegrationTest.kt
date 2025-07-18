package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.*
import de.org.dexterity.bookanything.shared.integrationtests.AbstractIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.*
import java.util.stream.IntStream

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GeoLocationControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var continentJpaRepository: ContinentJpaRepository
    @Autowired private lateinit var regionJpaRepository: RegionJpaRepository
    @Autowired private lateinit var countryJpaRepository: CountryJpaRepository
    @Autowired private lateinit var provinceJpaRepository: ProvinceJpaRepository
    @Autowired private lateinit var cityJpaRepository: CityJpaRepository
    @Autowired private lateinit var districtJpaRepository: DistrictJpaRepository

    @AfterEach
    fun tearDown() {
        districtJpaRepository.deleteAll()
        cityJpaRepository.deleteAll()
        provinceJpaRepository.deleteAll()
        countryJpaRepository.deleteAll()
        regionJpaRepository.deleteAll()
        continentJpaRepository.deleteAll()
    }

    @Test
    fun shouldCreateAndRetrieveAndDeleteOneOfEachGeoLocationHierarchy() {

        val genericPolygonAsString : String = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"

        val newContinentId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.CONTINENT, "Continent-1", genericPolygonAsString, null)
        val newRegionId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.REGION, "Region-1", genericPolygonAsString, newContinentId.id)
        val newCountryId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.COUNTRY, "Country-1", genericPolygonAsString, newRegionId.id)
        val newProvinceId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.PROVINCE, "Province-1", genericPolygonAsString, newCountryId.id)
        val newCityId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.CITY, "City-1", genericPolygonAsString, newProvinceId.id)
        val newDistrictId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.DISTRICT, "District-1", genericPolygonAsString, newCityId.id)

        // Deleting By Id
        deleteGeoLocationById(GeoLocationType.DISTRICT, newDistrictId)
        deleteGeoLocationById(GeoLocationType.CITY, newCityId)
        deleteGeoLocationById(GeoLocationType.PROVINCE, newProvinceId)
        deleteGeoLocationById(GeoLocationType.COUNTRY, newCountryId)
        deleteGeoLocationById(GeoLocationType.REGION, newRegionId)
        deleteGeoLocationById(GeoLocationType.CONTINENT, newContinentId)
    }

    @Test
    fun shouldDeleteAllGeoLocations() {

        val genericPolygonAsString : String = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"

        IntStream.range(1, 3).forEach { idxContinent ->
            val newContinentId: GeoLocationId =
                createOneGeoLocation(GeoLocationType.CONTINENT, "Continent-$idxContinent", genericPolygonAsString, null)

            IntStream.range(1, 3).forEach { idxRegion ->

                val newRegionId : GeoLocationId = createOneGeoLocation(
                    GeoLocationType.REGION,
                    "Region-$idxContinent.$idxRegion",
                    genericPolygonAsString,
                    newContinentId.id
                )

                IntStream.range(1, 3).forEach { idxCountry ->

                    val newCountryId : GeoLocationId = createOneGeoLocation(
                        GeoLocationType.COUNTRY,
                        "Country-$idxContinent.$idxRegion.$idxCountry",
                        genericPolygonAsString,
                        newRegionId.id
                    )

                    IntStream.range(1, 5).forEach { idxProvince ->

                        val newProvinceId : GeoLocationId = createOneGeoLocation(
                            GeoLocationType.PROVINCE,
                            "Province-$idxContinent.$idxRegion.$idxCountry.$idxProvince",
                            genericPolygonAsString,
                            newCountryId.id
                        )

                        IntStream.range(1, 3).forEach { idxCity ->

                            val newCityId : GeoLocationId = createOneGeoLocation(
                                GeoLocationType.CITY,
                                "City-$idxContinent.$idxRegion.$idxCountry.$idxCity",
                                genericPolygonAsString,
                                newProvinceId.id
                            )

                            IntStream.range(1, 3).forEach { idxDistrict ->

                                val newDistrictId : GeoLocationId = createOneGeoLocation(
                                    GeoLocationType.DISTRICT,
                                    "District-$idxContinent.$idxRegion.$idxCountry.$idxCity.$idxDistrict",
                                    genericPolygonAsString,
                                    newCityId.id
                                )

                            }  // for District

                        }  // for City

                    } // for Province

                }  // for Country

            } // for Region

        } // for Continent

        // Deleting All for each Type
        deleteAllGeoLocationsOfType(GeoLocationType.DISTRICT, 128)
        deleteAllGeoLocationsOfType(GeoLocationType.CITY, 64)
        deleteAllGeoLocationsOfType(GeoLocationType.PROVINCE, 32)
        deleteAllGeoLocationsOfType(GeoLocationType.COUNTRY, 8)
        deleteAllGeoLocationsOfType(GeoLocationType.REGION, 4)
        deleteAllGeoLocationsOfType(GeoLocationType.CONTINENT, 2)

    }

    @Test
    fun shouldReturnDeepGeoLocationHierarchyByIdOrName() {
        val genericPolygonAsString = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"

        // Create a hierarchy
        val newContinentId : GeoLocationId = createOneGeoLocation(GeoLocationType.CONTINENT, "DeepContinent", genericPolygonAsString, null)
        val newRegionId : GeoLocationId = createOneGeoLocation(GeoLocationType.REGION, "DeepRegion", genericPolygonAsString, newContinentId.id)
        val newCountryId : GeoLocationId = createOneGeoLocation(GeoLocationType.COUNTRY, "DeepCountry", genericPolygonAsString, newRegionId.id)
        val newProvinceId : GeoLocationId = createOneGeoLocation(GeoLocationType.PROVINCE, "DeepProvince", genericPolygonAsString, newCountryId.id)
        val newCityId : GeoLocationId = createOneGeoLocation(GeoLocationType.CITY, "DeepCity", genericPolygonAsString, newProvinceId.id)
        val newDistrictId : GeoLocationId = createOneGeoLocation(GeoLocationType.DISTRICT, "DeepDistrict", genericPolygonAsString, newCityId.id)

        // Test deep search by ID for Continent
        val deepContinentResult = mockMvc.get("/api/v1/geolocations/${GeoLocationType.CONTINENT.name.lowercase()}/deep-search?id=${newContinentId.id}") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val deepContinentResponse = objectMapper.readValue<DeepContinentResponse>(deepContinentResult.response.contentAsString)
        assertNotNull(deepContinentResponse)
        assertEquals("DeepContinent", deepContinentResponse.name)
        assertNotNull(deepContinentResponse.regionsList)
        assertEquals(1, deepContinentResponse.regionsList?.size)
        assertEquals("DeepRegion", deepContinentResponse.regionsList?.get(0)?.name)

        // Test deep search by Name for Country
        val deepCountryResult = mockMvc.get("/api/v1/geolocations/${GeoLocationType.COUNTRY.name.lowercase()}/deep-search?name=DeepCountry") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val deepCountryResponse = objectMapper.readValue<DeepCountryResponse>(deepCountryResult.response.contentAsString)
        assertNotNull(deepCountryResponse)
        assertEquals("DeepCountry", deepCountryResponse.name)
        assertNotNull(deepCountryResponse.provincesList)
        assertEquals(1, deepCountryResponse.provincesList?.size)
        assertEquals("DeepProvince", deepCountryResponse.provincesList?.get(0)?.name)
    }

    @Test
    fun `should return deep GeoLocation hierarchy by ID and Name`() {
        val genericPolygonAsString = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"

        // Create a hierarchy
        val continentId = createOneGeoLocation(GeoLocationType.CONTINENT, "DeepContinent", genericPolygonAsString, null)
        val regionId = createOneGeoLocation(GeoLocationType.REGION, "DeepRegion", genericPolygonAsString, continentId.id)
        val countryId = createOneGeoLocation(GeoLocationType.COUNTRY, "DeepCountry", genericPolygonAsString, regionId.id)
        val provinceId = createOneGeoLocation(GeoLocationType.PROVINCE, "DeepProvince", genericPolygonAsString, countryId.id)
        val cityId = createOneGeoLocation(GeoLocationType.CITY, "DeepCity", genericPolygonAsString, provinceId.id)
        val districtId = createOneGeoLocation(GeoLocationType.DISTRICT, "DeepDistrict", genericPolygonAsString, cityId.id)

        // Test deep search by ID for Continent
        val deepContinentResult = mockMvc.get("/api/v1/geolocations/${GeoLocationType.CONTINENT.name.lowercase()}/deep-search?id=${continentId.id}") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val deepContinentResponse = objectMapper.readValue<DeepContinentResponse>(deepContinentResult.response.contentAsString)
        assertNotNull(deepContinentResponse)
        assertEquals("DeepContinent", deepContinentResponse.name)
        assertNotNull(deepContinentResponse.regionsList)
        assertEquals(1, deepContinentResponse.regionsList?.size)
        assertEquals("DeepRegion", deepContinentResponse.regionsList?.get(0)?.name)
        // Parent should be null in the deep response to avoid StackOverflow
        assertNull(deepContinentResponse.regionsList?.get(0)?.countriesList?.get(0)?.provincesList?.get(0)?.citiesList?.get(0)?.districtsList?.get(0)?.addressesList?.get(0)?.district)


        // Test deep search by Name for Country
        val deepCountryResult = mockMvc.get("/api/v1/geolocations/${GeoLocationType.COUNTRY.name.lowercase()}/deep-search?name=DeepCountry") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val deepCountryResponse = objectMapper.readValue<DeepCountryResponse>(deepCountryResult.response.contentAsString)
        assertNotNull(deepCountryResponse)
        assertEquals("DeepCountry", deepCountryResponse.name)
        assertNotNull(deepCountryResponse.provincesList)
        assertEquals(1, deepCountryResponse.provincesList?.size)
        assertEquals("DeepProvince", deepCountryResponse.provincesList?.get(0)?.name)
        // Parent should be null in the deep response to avoid StackOverflow
        assertNull(deepCountryResponse.provincesList?.get(0)?.citiesList?.get(0)?.districtsList?.get(0)?.addressesList?.get(0)?.district)
    }

    private fun testOneGeoLocationCreationAndRecovery(
        geoLocationType: GeoLocationType,
        geoLocationName: String,
        polygonAsString: String,
        parentId : Long?
    ): GeoLocationId {

        // 1. Create
        val createRequest = CreateGeoLocationRequest(name = geoLocationName, boundaryRepresentation = polygonAsString, parentId = parentId)

        val createResult = mockMvc.post("/api/v1/geolocations/" + geoLocationType.name.lowercase()) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
            with(jwt())
        }.andExpect { status { isOk() } }
            .andReturn()

        val createdResponse = objectMapper.readValue<GeoLocationResponse>(createResult.response.contentAsString)
        println("===> GeoLocation Created: $createdResponse")
        assertNotNull(createdResponse.id)
        assertEquals(geoLocationName, createdResponse.name)

        // 2. Retrieve by ID
        val findByIdResult = mockMvc.get("/api/v1/geolocations/" + geoLocationType.name.lowercase() + "/${createdResponse.id}") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val foundByIdResponse = objectMapper.readValue<GeoLocationResponse>(findByIdResult.response.contentAsString)
        assertEquals(createdResponse.id, foundByIdResponse.id)
        assertEquals(geoLocationName, foundByIdResponse.name)

        // 3. Update
        val updatedGeoLocationName : String = "Updated - $geoLocationName"
        val updatedPolygonAreaAsString : String = "POLYGON ((15 15, 15 25, 25 25, 25 15, 15 15))"
        val updateRequest = UpdateGeoLocationRequest(name = updatedGeoLocationName, boundaryRepresentation = updatedPolygonAreaAsString, parentId = parentId)

        val updateResult = mockMvc.put("/api/v1/geolocations/" + geoLocationType.name.lowercase() + "/${createdResponse.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val updatedResponse = objectMapper.readValue<GeoLocationResponse>(updateResult.response.contentAsString)
        println("===> GeoLocation Updated: $updatedResponse")
        assertEquals(createdResponse.id, updatedResponse.id)
        assertEquals(updatedGeoLocationName, updatedResponse.name)
        assertEquals(updatedPolygonAreaAsString, updatedResponse.boundaryRepresentation)


        // 4. Retrieve all
        val findAllResult = mockMvc.get("/api/v1/geolocations/" + geoLocationType.name.lowercase()) {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val foundAllResponse = objectMapper.readValue<List<GeoLocationResponse>>(findAllResult.response.contentAsString)
        assertEquals(1, foundAllResponse.size)
        assertEquals(updatedGeoLocationName, foundAllResponse[0].name)
        assertEquals(updatedPolygonAreaAsString, foundAllResponse[0].boundaryRepresentation)

        return GeoLocationId(createdResponse.id)
    }

    private fun createOneGeoLocation(geoLocationType: GeoLocationType, geoLocationName: String, polygonAsString: String, parentId : Long?): GeoLocationId {

        // 1. Create
        val createRequest = CreateGeoLocationRequest(name = geoLocationName, boundaryRepresentation = polygonAsString, parentId = parentId)

        val createResult = mockMvc.post("/api/v1/geolocations/" + geoLocationType.name.lowercase()) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
            with(jwt())
        }.andExpect { status { isOk() } }
            .andReturn()

        val createdResponse = objectMapper.readValue<GeoLocationResponse>(createResult.response.contentAsString)
        println("===> GeoLocation Created: $createdResponse")
        assertNotNull(createdResponse.id)
        assertEquals(geoLocationName, createdResponse.name)

        // 2. Retrieve by ID
        val findByIdResult = mockMvc.get("/api/v1/geolocations/" + geoLocationType.name.lowercase() + "/${createdResponse.id}") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val foundByIdResponse = objectMapper.readValue<GeoLocationResponse>(findByIdResult.response.contentAsString)
        assertEquals(createdResponse.id, foundByIdResponse.id)
        assertEquals(geoLocationName, foundByIdResponse.name)

        return GeoLocationId(createdResponse.id)
    }

    private fun deleteGeoLocationById(geoLocationType: GeoLocationType, targetGeoLocationId: GeoLocationId) {

        // 1. Delete
        mockMvc.delete("/api/v1/geolocations/" + geoLocationType.name.lowercase() + "/${targetGeoLocationId.id}") {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        // 2. Verify deletion
        mockMvc.get("/api/v1/geolocations/" + geoLocationType.name.lowercase() + "/${targetGeoLocationId.id}") {
            with(jwt())
        }.andExpect { status { isNotFound() } }

    }

    private fun deleteAllGeoLocationsOfType(targetGeoLocationType: GeoLocationType, existingRowsCount: Int) {

        // Retrieve all
        var findAllResult = mockMvc.get("/api/v1/geolocations/" + targetGeoLocationType.name.lowercase()) {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        var foundAllResponse = objectMapper.readValue<List<GeoLocationResponse>>(findAllResult.response.contentAsString)
        assertEquals(existingRowsCount, foundAllResponse.size)

        // 1. Delete
        mockMvc.delete("/api/v1/geolocations/" + targetGeoLocationType.name.lowercase() + "/all") {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        // 2. Verify deletion
        // Retrieve all
        findAllResult = mockMvc.get("/api/v1/geolocations/" + targetGeoLocationType.name.lowercase()) {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        foundAllResponse = objectMapper.readValue<List<GeoLocationResponse>>(findAllResult.response.contentAsString)
        assertEquals(0, foundAllResponse.size)

    }

}