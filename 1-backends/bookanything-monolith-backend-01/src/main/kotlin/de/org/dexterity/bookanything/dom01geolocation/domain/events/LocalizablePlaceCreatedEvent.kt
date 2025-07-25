package de.org.dexterity.bookanything.dom01geolocation.domain.events

import java.time.LocalDateTime
import java.util.UUID

data class LocalizablePlaceCreatedEvent(
    val id: UUID,
    val friendlyId: String,
    val name: String,
    val alias: String?,
    val propertiesDetailsMap: Map<String, Any>?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
