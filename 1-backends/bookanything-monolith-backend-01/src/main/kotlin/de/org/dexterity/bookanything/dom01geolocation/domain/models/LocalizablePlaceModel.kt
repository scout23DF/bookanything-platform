package de.org.dexterity.bookanything.dom01geolocation.domain.models

import org.locationtech.jts.geom.Point
import java.util.UUID

data class LocalizablePlaceModel(
    val id: UUID,
    val friendlyId: String,
    val name: String,
    val alias: String?,
    val additionalDetailsMap: Map<String, Any>?,
    val locationPoint: Point
)