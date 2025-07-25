package de.org.dexterity.bookanything.dom01geolocation.domain.ports

/**
 * Port for publishing events to a message broker.
 * This acts as an abstraction layer over the specific messaging technology (e.g., Kafka).
 */
interface EventPublisherPort {
    fun publish(event: Any)
}
