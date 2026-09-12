package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonDownloaderUseCase
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonImporterUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryDataToImportRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonDownloadRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonDownloadResponse
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.HierarchyDetailsRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoJsonDownloadRequestedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.*

class GeoJsonDownloaderControllerTest {

    private var geoJsonDownloaderUseCase: GeoJsonDownloaderUseCase = mockk()
    private var geoJsonImporterUseCase: GeoJsonImporterUseCase = mockk()

    private lateinit var targetController: GeoJsonDownloaderController

    @BeforeEach
    fun setUp() {
        targetController = GeoJsonDownloaderController(geoJsonDownloaderUseCase, geoJsonImporterUseCase)
    }

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
        val responseDto = GeoJsonDownloadResponse(jobId, "GeoJSON download job accepted. It will be processed asynchronously.")

        every { geoJsonDownloaderUseCase.initiateDownload(request) } returns event

        val response = targetController.downloadGeoJsonFiles(request)

        assertEquals(HttpStatus.ACCEPTED, response.statusCode)
        assertEquals(responseDto, response.body)
        verify(exactly = 1) { geoJsonDownloaderUseCase.initiateDownload(request) }

    }

    /*
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
            .andExpect(status().isBadRequest)
    }
    */
}