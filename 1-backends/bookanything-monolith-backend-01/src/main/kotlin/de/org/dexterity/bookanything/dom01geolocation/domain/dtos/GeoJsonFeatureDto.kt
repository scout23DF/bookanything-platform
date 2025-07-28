package de.org.dexterity.bookanything.dom01geolocation.domain.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.locationtech.jts.geom.Geometry

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoJsonFeatureDto(

    val geometry: Geometry,
    val properties: Map<String, Any?>

)
