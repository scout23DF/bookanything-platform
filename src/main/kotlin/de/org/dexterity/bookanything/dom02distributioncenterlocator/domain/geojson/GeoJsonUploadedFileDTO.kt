package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.geojson

import org.geojson.FeatureCollection
import org.locationtech.jts.geom.Point
import java.util.UUID

data class GeoJsonUploadedFileDTO(
    val contentDataType: String,
    val type: String,
    val featureCollection: FeatureCollection
)
