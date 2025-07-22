package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.messaging.kafkapublishers

import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoLocationEnrichmentEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlaceCreatedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlaceDeletedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlacesAllDeletedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaEventPublisherAdapter(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : EventPublisherPort {

    override fun publish(event: Any) {
        val topic = when (event) {
            is LocalizablePlaceCreatedEvent -> "localizable-place-created-topic"
            is LocalizablePlaceDeletedEvent -> "localizable-place-deleted-topic"
            is LocalizablePlacesAllDeletedEvent -> "localizable-places-all-deleted-topic"
            is GeoLocationEnrichmentEvent -> "geolocation-enrichment-request-topic"
            else -> throw IllegalArgumentException("Unknown event type: ${event.javaClass.name}")
        }
        kafkaTemplate.send(topic, event)
        kafkaTemplate.flush()
    }
}