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
import org.springframework.data.domain.Page
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

        val newContinentId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.CONTINENT, "Continent-1", "continent-1", genericPolygonAsString, null)
        val newRegionId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.REGION, "Region-1", "region-1", genericPolygonAsString, newContinentId.id)
        val newCountryId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.COUNTRY, "Country-1", "country-1", genericPolygonAsString, newRegionId.id)
        val newProvinceId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.PROVINCE, "Province-1", "province-1", genericPolygonAsString, newCountryId.id)
        val newCityId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.CITY, "City-1", "city-1", genericPolygonAsString, newProvinceId.id)
        val newDistrictId : GeoLocationId = testOneGeoLocationCreationAndRecovery(GeoLocationType.DISTRICT, "District-1", "district-1", genericPolygonAsString, newCityId.id)

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
                createOneGeoLocation(GeoLocationType.CONTINENT, "Continent-$idxContinent", "continent-$idxContinent", genericPolygonAsString, null)

            IntStream.range(1, 3).forEach { idxRegion ->

                val newRegionId : GeoLocationId = createOneGeoLocation(
                    GeoLocationType.REGION,
                    "Region-$idxContinent.$idxRegion",
                    "region-$idxContinent.$idxRegion",
                    genericPolygonAsString,
                    newContinentId.id
                )

                IntStream.range(1, 3).forEach { idxCountry ->

                    val newCountryId : GeoLocationId = createOneGeoLocation(
                        GeoLocationType.COUNTRY,
                        "Country-$idxContinent.$idxRegion.$idxCountry",
                        "country-$idxContinent.$idxRegion.$idxCountry",
                        genericPolygonAsString,
                        newRegionId.id
                    )

                    IntStream.range(1, 5).forEach { idxProvince ->

                        val newProvinceId : GeoLocationId = createOneGeoLocation(
                            GeoLocationType.PROVINCE,
                            "Province-$idxContinent.$idxRegion.$idxCountry.$idxProvince",
                            "province-$idxContinent.$idxRegion.$idxCountry.$idxProvince",
                            genericPolygonAsString,
                            newCountryId.id
                        )

                        IntStream.range(1, 3).forEach { idxCity ->

                            val newCityId : GeoLocationId = createOneGeoLocation(
                                GeoLocationType.CITY,
                                "City-$idxContinent.$idxRegion.$idxCountry.$idxCity",
                                "city-$idxContinent.$idxRegion.$idxCountry.$idxCity",
                                genericPolygonAsString,
                                newProvinceId.id
                            )

                            IntStream.range(1, 3).forEach { idxDistrict ->

                                val newDistrictId : GeoLocationId = createOneGeoLocation(
                                    GeoLocationType.DISTRICT,
                                    "District-$idxContinent.$idxRegion.$idxCountry.$idxCity.$idxDistrict",
                                    "district-$idxContinent.$idxRegion.$idxCountry.$idxCity.$idxDistrict",
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
        val newContinentId : GeoLocationId = createOneGeoLocation(GeoLocationType.CONTINENT, "DeepContinent", "deep-continent", genericPolygonAsString, null)
        val newRegionId : GeoLocationId = createOneGeoLocation(GeoLocationType.REGION, "DeepRegion", "deep-region", genericPolygonAsString, newContinentId.id)
        val newCountryId : GeoLocationId = createOneGeoLocation(GeoLocationType.COUNTRY, "DeepCountry", "deep-country", genericPolygonAsString, newRegionId.id)
        val newProvinceId : GeoLocationId = createOneGeoLocation(GeoLocationType.PROVINCE, "DeepProvince", "deep-province", genericPolygonAsString, newCountryId.id)
        val newCityId : GeoLocationId = createOneGeoLocation(GeoLocationType.CITY, "DeepCity", "deep-city", genericPolygonAsString, newProvinceId.id)
        val newDistrictId : GeoLocationId = createOneGeoLocation(GeoLocationType.DISTRICT, "DeepDistrict", "deep-district", genericPolygonAsString, newCityId.id)

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

    private fun testOneGeoLocationCreationAndRecovery(
        geoLocationType: GeoLocationType,
        geoLocationName: String,
        friendlyId: String,
        polygonAsString: String,
        parentId : Long?
    ): GeoLocationId {

        // 1. Create
        val createRequest = CreateGeoLocationRequest(friendlyId = friendlyId, name = geoLocationName, boundaryRepresentation = polygonAsString, parentId = parentId)

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
        val updatedFriendlyId : String = "updated-$friendlyId"
        val updatedPolygonAreaAsString : String = "POLYGON ((15 15, 15 25, 25 25, 25 15, 15 15))"
        val updateRequest = UpdateGeoLocationRequest(friendlyId = updatedFriendlyId, name = updatedGeoLocationName, boundaryRepresentation = updatedPolygonAreaAsString, parentId = parentId)

        val updateResult = mockMvc.put("/api/v1/geolocations/" + geoLocationType.name.lowercase() + "/${createdResponse.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val updatedResponse = objectMapper.readValue<GeoLocationResponse>(updateResult.response.contentAsString)
        println("===> GeoLocation Updated: $updatedResponse")
        assertEquals(createdResponse.id, updatedResponse.id)
        assertEquals(updatedGeoLocationName, updatedResponse.name)
        assertEquals(updatedFriendlyId, updatedResponse.friendlyId)
        assertEquals(updatedPolygonAreaAsString, updatedResponse.boundaryRepresentation)


        // 4. Retrieve all
        val findAllResult = mockMvc.get(
            "/api/v1/geolocations/" + geoLocationType.name.lowercase() + "?includeBoundary=true"
        ) {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val foundAllResponse = objectMapper.readValue<Page<GeoLocationResponse>>(findAllResult.response.contentAsString)
        assertEquals(1, foundAllResponse.content.size)
        assertEquals(updatedGeoLocationName, foundAllResponse.content[0].name)
        assertEquals(updatedFriendlyId, foundAllResponse.content[0].friendlyId)
        assertEquals(updatedPolygonAreaAsString, foundAllResponse.content[0].boundaryRepresentation)

        return GeoLocationId(createdResponse.id)
    }

    private fun createOneGeoLocation(geoLocationType: GeoLocationType, geoLocationName: String, friendlyId: String, polygonAsString: String, parentId : Long?): GeoLocationId {

        // 1. Create
        val createRequest = CreateGeoLocationRequest(friendlyId = friendlyId, name = geoLocationName, boundaryRepresentation = polygonAsString, parentId = parentId)

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

        var foundAllResponse = objectMapper.readValue<Page<GeoLocationResponse>>(findAllResult.response.contentAsString)
        assertEquals(existingRowsCount, foundAllResponse.totalElements.toInt())

        // 1. Delete
        mockMvc.delete("/api/v1/geolocations/" + targetGeoLocationType.name.lowercase() + "/all") {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        // 2. Verify deletion
        // Retrieve all
        findAllResult = mockMvc.get("/api/v1/geolocations/" + targetGeoLocationType.name.lowercase()) {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        foundAllResponse = objectMapper.readValue<Page<GeoLocationResponse>>(findAllResult.response.contentAsString)
        assertEquals(0, foundAllResponse.content.size)

    }

    @Test
    fun shouldSearchByFriendlyId() {
        val genericPolygonAsString = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"
        createOneGeoLocation(GeoLocationType.CONTINENT, "SearchContinent", "search-continent", genericPolygonAsString, null)

        val result = mockMvc.get("/api/v1/geolocations/${GeoLocationType.CONTINENT.name.lowercase()}/search-by-friendlyid?friendlyId=search") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val response = objectMapper.readValue<Page<GeoLocationResponse>>(result.response.contentAsString)
        assertEquals(1, response.content.size)
        assertEquals("SearchContinent", response.content[0].name)
    }

    @Test
    fun shouldSearchByAdditionalDetail() {
        val genericPolygonAsString = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))"
        val createRequest = CreateGeoLocationRequest(
            friendlyId = "prop-continent",
            name = "PropContinent",
            boundaryRepresentation = genericPolygonAsString,
            additionalDetailsMap = mapOf("key1" to "value1")
        )

        mockMvc.post("/api/v1/geolocations/${GeoLocationType.CONTINENT.name.lowercase()}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
            with(jwt())
        }.andExpect { status { isOk() } }

        val result = mockMvc.get("/api/v1/geolocations/${GeoLocationType.CONTINENT.name.lowercase()}/search-by-additional-detail?key=key1&value=value1") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val response = objectMapper.readValue<Page<GeoLocationResponse>>(result.response.contentAsString)
        assertEquals(1, response.content.size)
        assertEquals("PropContinent", response.content[0].name)
    }

}
