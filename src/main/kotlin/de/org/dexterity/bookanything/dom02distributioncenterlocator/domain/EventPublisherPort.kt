package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain

interface EventPublisherPort {
    fun publish(event: Any)
}
