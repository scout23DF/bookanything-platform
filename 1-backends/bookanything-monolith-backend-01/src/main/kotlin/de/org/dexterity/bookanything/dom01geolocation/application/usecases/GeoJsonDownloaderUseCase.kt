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
        require(request.countryDataToImportRequestList.all { it.levels.all { it in 0..4 } }) {
            "Levels must be between 0 and 4."
        }

        val jobId = UUID.randomUUID()
        val event = GeoJsonDownloadRequestedEvent(
            jobId = jobId,
            request = request
        )

        eventPublisher.publish(event)

        return event
    }
}
