package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

import java.util.*

data class LocalizablePlaceRestResponse(
    val id: UUID,
    val name: String,
    val latitude: Double,
    val longitude: Double
)