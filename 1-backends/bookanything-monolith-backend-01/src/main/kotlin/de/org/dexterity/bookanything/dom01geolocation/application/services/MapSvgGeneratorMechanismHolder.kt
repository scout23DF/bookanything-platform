package de.org.dexterity.bookanything.dom01geolocation.application.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Current map SVG generator mechanism (KOTLIN or GEOPANDAS). The import workflow sets it through its
 * `configureMapSvgGeneratorMechanism` activity; the artifacts activities read it when they run, so the
 * choice made for an import also applies to the per-GeoLocation workflows started by the Kafka consumer.
 */
@Component
class MapSvgGeneratorMechanismHolder(
    @Value("\${application.domain-settings.geolocation.map-svg-generator-mechanism:KOTLIN}")
    configured: String = "KOTLIN"
) {
    @Volatile
    var current: String = configured
}
