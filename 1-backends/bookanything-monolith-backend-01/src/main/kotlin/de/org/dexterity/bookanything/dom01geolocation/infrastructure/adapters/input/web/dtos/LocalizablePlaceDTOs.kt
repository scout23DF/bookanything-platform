package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

import jakarta.validation.constraints.Size
import java.util.*

// --- Request DTOs ---

data class CreateLocalizablePlaceRestRequest(
    val name: String,
    @field:Size(max = 20)
    val alias: String? = null,
    val latitude: Double,
    val longitude: Double
)


// --- Response DTOs ---
data class LocalizablePlaceRestResponse(
    val id: UUID,
    val name: String,
    val alias: String?,
    val latitude: Double,
    val longitude: Double
)
