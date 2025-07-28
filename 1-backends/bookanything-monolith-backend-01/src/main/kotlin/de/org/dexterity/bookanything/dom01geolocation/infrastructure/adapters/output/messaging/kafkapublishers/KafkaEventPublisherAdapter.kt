package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.messaging.kafkapublishers

import de.org.dexterity.bookanything.dom01geolocation.domain.events.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.events.AssetRegisteredEvent
import de.org.dexterity.bookanything.dom02assetmanager.domain.events.AssetUploadedToStorageEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaEventPublisherAdapter(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    @Value("\${topics.geolocation.geojson-download.country-data-required}") private val countryDataRequiredTopic: String,
    @Value("\${topics.geolocation.geojson-file.downloaded}") private val fileDownloadedTopic: String,
    @Value("\${topics.geolocation.geojson-download.failed}") private val downloadFailedTopic: String,
    @Value("\${topics.geolocation.geojson-download.requested}") private val downloadRequestedTopic: String,
    @Value("\${topics.geolocation.geojson-imported-file.ready-to-make-geo-locations}") private val geoJsonImportedFileReadyToMakeGeoLocationsTopic: String,
    @Value("\${topics.asset-manager.asset-creation.registered}") private val assetRegisteredTopic: String,
    @Value("\${topics.asset-manager.asset-creation.uploaded-to-storage}") private val assetUploadedToStorageTopic: String
) : EventPublisherPort {

    override fun publish(event: Any) {
        val topic = when (event) {
            is LocalizablePlaceCreatedEvent -> "localizable-place-created-topic"
            is LocalizablePlaceDeletedEvent -> "localizable-place-deleted-topic"
            is LocalizablePlacesAllDeletedEvent -> "localizable-places-all-deleted-topic"
            is GeoLocationEnrichmentEvent -> "geolocation-enrichment-request-topic"
            is CountryGeoJsonDataRequiredEvent -> countryDataRequiredTopic
            is GeoJsonFileDownloadedEvent -> fileDownloadedTopic
            is GeoJsonDownloadFailedEvent -> downloadFailedTopic
            is GeoJsonDownloadRequestedEvent -> downloadRequestedTopic
            is AssetRegisteredEvent -> assetRegisteredTopic
            is AssetUploadedToStorageEvent -> assetUploadedToStorageTopic
            is CountryDataToMakeGeoLocationsEvent -> geoJsonImportedFileReadyToMakeGeoLocationsTopic
            else -> throw IllegalArgumentException("Unknown event type: ${event.javaClass.name}")
        }
        kafkaTemplate.send(topic, event)
        kafkaTemplate.flush()
    }
}