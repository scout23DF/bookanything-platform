package de.org.dexterity.bookanything.dom01geolocation.domain.models

import org.locationtech.jts.geom.Geometry
import java.time.Instant
import java.util.UUID

enum class GeoJsonImportStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}

data class GeoJsonImported(
    val id: UUID = UUID.randomUUID(),
    val fileName: String,
    val originalContentType: String,
    val importTimestamp: Instant = Instant.now(),
    var status: GeoJsonImportStatus = GeoJsonImportStatus.PENDING,
    var statusDetails: String? = null,
    var featuresList: List<GeoJsonFeature> = emptyList()
)

data class GeoJsonFeature(
    val id: UUID = UUID.randomUUID(),
    val geoJsonImportedFileId: UUID,
    val featureGeometry: Geometry,
    val featurePropertiesMap: Map<String, Any?>
)
