package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

data class CreateLocalizablePlaceRestRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double
)