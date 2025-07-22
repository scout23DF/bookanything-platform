package de.org.dexterity.bookanything.dom01geolocation.domain.events

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType

data class GeoLocationEnrichmentEvent(
    val id: Long,
    val type: GeoLocationType
)