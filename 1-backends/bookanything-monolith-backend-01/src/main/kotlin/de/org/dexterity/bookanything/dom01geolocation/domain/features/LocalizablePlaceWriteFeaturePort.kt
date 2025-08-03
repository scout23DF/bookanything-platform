package de.org.dexterity.bookanything.dom01geolocation.domain.features

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import org.locationtech.jts.geom.Point
import java.util.*

interface LocalizablePlaceWriteFeaturePort {

    fun create(
        locationName: String,
        locationAlias: String?,
        friendlyId: String,
        propertiesDetailsMap: Map<String, Any>?,
        locationPoint: Point
    ): LocalizablePlaceModel

    fun removeById(id: UUID)
    fun synchronizeAll(): Map<String, Int>
    fun removeAll()
}
