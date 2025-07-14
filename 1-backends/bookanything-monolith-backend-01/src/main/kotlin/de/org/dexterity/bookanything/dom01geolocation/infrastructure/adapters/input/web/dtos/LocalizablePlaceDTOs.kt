package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

import java.util.*

// --- Request DTOs ---

data class CreateLocalizablePlaceRestRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double
)


// --- Response DTOs ---
data class LocalizablePlaceRestResponse(
    val id: UUID,
    val name: String,
    val latitude: Double,
    val longitude: Double
)
