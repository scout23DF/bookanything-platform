package de.org.dexterity.bookanything.dom01geolocation.domain.ports

interface EventPublisherPort {
    fun publish(event: Any)
}