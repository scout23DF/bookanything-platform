package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType

// --- Request DTOs ---
data class CreateGeoLocationRequest(
    val name: String,
    val alias: String? = null,
    val boundaryRepresentation: String? = null,
    val parentId: Long? = null
)
data class UpdateGeoLocationRequest(
    val name: String,
    val alias: String? = null,
    val boundaryRepresentation: String? = null,
    val parentId: Long? = null
)

// --- Response DTOs ---
data class GeoLocationResponse(
    val type: GeoLocationType,
    val id: Long,
    val name: String,
    val alias: String? = null,
    val boundaryRepresentation: String? = null,
    val parentId: Long? = null
)
