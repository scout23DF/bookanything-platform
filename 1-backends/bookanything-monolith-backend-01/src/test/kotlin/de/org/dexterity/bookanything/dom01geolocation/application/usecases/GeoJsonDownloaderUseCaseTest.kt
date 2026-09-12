package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryDataToImportRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonDownloadRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.HierarchyDetailsRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoJsonDownloadRequestedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.util.*

class GeoJsonDownloaderUseCaseTest {

    private val eventPublisherPort: EventPublisherPort = mockk()

    @InjectMockKs
    private lateinit var geoJsonDownloaderUseCase: GeoJsonDownloaderUseCase

    @BeforeEach
    fun setUp() {
        geoJsonDownloaderUseCase = GeoJsonDownloaderUseCase(eventPublisherPort)
    }

    @Test
    fun `initiateDownload SHOULD publish event WHEN request is valid`() {
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

        every { eventPublisherPort.publish(any()) } returns Unit

        val result = geoJsonDownloaderUseCase.initiateDownload(request)

        verify(exactly = 1) { eventPublisherPort.publish(any()) }
        assertEquals(request, result.geoJsonDownloadRequest)
        assertNotNull(result.jobId)
    }

    @Test
    fun `initiateDownload SHOULD throw IllegalArgumentException WHEN country list is empty`() {
        // GIVEN
        val request = GeoJsonDownloadRequest(countryDataToImportRequestList = emptyList())

        // WHEN & THEN
        assertThrows<IllegalArgumentException> {
            geoJsonDownloaderUseCase.initiateDownload(request)
        }
    }

    @Test
    fun `initiateDownload SHOULD throw IllegalArgumentException WHEN country code is invalid`() {
        // GIVEN
        val request = GeoJsonDownloadRequest(
            countryDataToImportRequestList = listOf(
                CountryDataToImportRequest(
                    countryIso3Code = "BR",
                    importingDetailsForCountry = HierarchyDetailsRequest(hierarchyType = "COUNTRY", parentAliasToAttach = "ROOT")
                )
            )
        )

        // WHEN & THEN
        assertThrows<IllegalArgumentException> {
            geoJsonDownloaderUseCase.initiateDownload(request)
        }
    }

    @Test
    fun `initiateDownload SHOULD throw IllegalArgumentException WHEN no import details are provided`() {
        // GIVEN
        val request = GeoJsonDownloadRequest(
            countryDataToImportRequestList = listOf(
                CountryDataToImportRequest(countryIso3Code = "BRA")
            )
        )

        // WHEN & THEN
        assertThrows<IllegalArgumentException> {
            geoJsonDownloaderUseCase.initiateDownload(request)
        }
    }

    @Test
    fun `initiateDownload SHOULD throw IllegalArgumentException WHEN hierarchy level is invalid`() {
        // GIVEN
        val request = GeoJsonDownloadRequest(
            countryDataToImportRequestList = listOf(
                CountryDataToImportRequest(
                    countryIso3Code = "BRA",
                    importingDetailsForCountry = HierarchyDetailsRequest(hierarchyType = "COUNTRY", parentAliasToAttach = "ROOT", hierarchyLevelOfFileToImport = 5)
                )
            )
        )

        // WHEN & THEN
        assertThrows<IllegalArgumentException> {
            geoJsonDownloaderUseCase.initiateDownload(request)
        }
    }

}