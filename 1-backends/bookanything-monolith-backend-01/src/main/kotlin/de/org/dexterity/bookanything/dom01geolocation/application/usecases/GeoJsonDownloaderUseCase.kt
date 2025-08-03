package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonDownloadRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoJsonDownloadRequestedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import org.springframework.stereotype.Service
import java.util.*

@Service
class GeoJsonDownloaderUseCase(
    private val eventPublisher: EventPublisherPort
) {

    fun initiateDownload(request: GeoJsonDownloadRequest): GeoJsonDownloadRequestedEvent {
        // Basic validation
        require(request.countryDataToImportRequestList.isNotEmpty()) { "Country list cannot be empty." }
        require(request.countryDataToImportRequestList.all { it.countryIso3Code.length == 3 && it.countryIso3Code.all(Char::isUpperCase) }) {
            "All country codes must be 3-letter uppercase ISO codes."
        }

        require(request.countryDataToImportRequestList.all {
            it.importingDetailsForCountry != null || it.importingDetailsForProvince != null || it.importingDetailsForCity != null || it.importingDetailsForDistrict != null
        }) {
            "At least, one ImportingDetailsRequest must be provided. Choose the GeoLocation Hieraychy Type you want to import: Country | Province | City | District."
        }

        request.countryDataToImportRequestList.forEach { oneCountryRequest ->
            if (oneCountryRequest.importingDetailsForCountry != null) {
                require(oneCountryRequest.importingDetailsForCountry.hierarchyLevelOfFileToImport in 0..4) {
                    "For the Country [${oneCountryRequest.countryIso3Code}], in the 'ImportingDetailsForCountry', the hierarchyLevelOfFileToImport field must be between 0 and 4."
                }
            }

            if (oneCountryRequest.importingDetailsForProvince != null) {
                require(oneCountryRequest.importingDetailsForProvince.hierarchyLevelOfFileToImport in 0..4) {
                    "For the Country [${oneCountryRequest.countryIso3Code}], in the 'ImportingDetailsForProvince', the hierarchyLevelOfFileToImport field must be between 0 and 4."
                }
            }

            if (oneCountryRequest.importingDetailsForCity != null) {
                require(oneCountryRequest.importingDetailsForCity.hierarchyLevelOfFileToImport in 0..4) {
                    "For the Country [${oneCountryRequest.countryIso3Code}], in the 'ImportingDetailsForCity', the hierarchyLevelOfFileToImport field must be between 0 and 4."
                }
            }

            if (oneCountryRequest.importingDetailsForDistrict != null) {
                require(oneCountryRequest.importingDetailsForDistrict.hierarchyLevelOfFileToImport in 0..4) {
                    "For the Country [${oneCountryRequest.countryIso3Code}], in the 'ImportingDetailsForDistrict', the hierarchyLevelOfFileToImport field must be between 0 and 4."
                }
            }

        }

        val jobId = UUID.randomUUID()
        val event = GeoJsonDownloadRequestedEvent(
            jobId = jobId,
            geoJsonDownloadRequest = request
        )

        eventPublisher.publish(event)

        return event
    }
}
