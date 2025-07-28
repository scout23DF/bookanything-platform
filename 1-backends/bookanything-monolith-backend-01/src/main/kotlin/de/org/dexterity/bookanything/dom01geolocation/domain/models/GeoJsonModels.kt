package de.org.dexterity.bookanything.dom01geolocation.domain.models

import org.geolatte.geom.Geometry
import java.time.Instant
import java.util.*

enum class GeoJsonImportStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}

data class GeoJsonImportedFileModel(
    val id: UUID = UUID.randomUUID(),
    val fileName: String,
    val originalContentType: String,
    val importTimestamp: Instant = Instant.now(),
    var status: GeoJsonImportStatus = GeoJsonImportStatus.PENDING,
    var statusDetails: String? = null,
    var featuresList: List<GeoJsonFeatureModel> = emptyList()
)

data class GeoJsonFeatureModel(
    val id: UUID = UUID.randomUUID(),
    val geoJsonImportedFile: GeoJsonImportedFileModel,
    val featureGeometry: Geometry<*>? = null,
    val featurePropertiesMap: Map<String, Any?>,
    val featureGeometryContentAsJson: String? = null
)
