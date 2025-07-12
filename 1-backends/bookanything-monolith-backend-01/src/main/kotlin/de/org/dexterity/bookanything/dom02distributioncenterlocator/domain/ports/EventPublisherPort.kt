package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports

interface EventPublisherPort {
    fun publish(event: Any)
}