package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonDownloaderUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryDataToImportRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonDownloadRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.HierarchyDetailsRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoJsonDownloadRequestedEvent
import de.org.dexterity.bookanything.shared.integrationtests.AbstractIntegrationTest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GeoJsonDownloaderControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private var geoJsonDownloaderUseCase: GeoJsonDownloaderUseCase = mockk()


    @Test
    fun `downloadGeoJsonFiles SHOULD return 202 Accepted WHEN request is valid`() {
        // GIVEN
        val request = GeoJsonDownloadRequest(
            countryDataToImportRequestList = listOf(
                CountryDataToImportRequest(
                    countryIso3Code = "BRA",
                    importingDetailsForCountry = HierarchyDetailsRequest(hierarchyType = "COUNTRY", parentAliasToAttach = "ROOT")
                )
            )
        )

        val jobId = UUID.randomUUID()
        val event = GeoJsonDownloadRequestedEvent(jobId = jobId, geoJsonDownloadRequest = request)

        every { geoJsonDownloaderUseCase.initiateDownload(request) } returns event

        // WHEN & THEN
        mockMvc.perform(
            post("/api/v1/geolocation/geojson/downloads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isAccepted)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.jobId").isNotEmpty())
            .andExpect(jsonPath("$.message").value("GeoJSON download job accepted. It will be processed asynchronously."))
    }

    @Test
    fun `downloadGeoJsonFiles SHOULD return 400 Bad Request WHEN request body is malformed`() {
        // GIVEN
        val malformedRequest = "{ \"invalidJson\": true }"

        // WHEN & THEN
        mockMvc.perform(
            post("/api/v1/geolocation/geojson/downloads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedRequest)
        )
            .andExpect(status().is5xxServerError)
    }

}