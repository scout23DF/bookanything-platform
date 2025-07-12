package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.dtos

import org.geojson.FeatureCollection

data class GeoJsonUploadedFileDTO(
    val contentDataType: String,
    val type: String,
    val featureCollection: FeatureCollection
)