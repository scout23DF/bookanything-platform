package de.org.dexterity.bookanything.dom01geolocation.domain.features

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import org.locationtech.jts.geom.Point
import java.util.UUID

interface LocalizablePlaceReadFeaturePort {
    fun searchById(id: UUID): LocalizablePlaceModel?
    fun searchAll(): List<LocalizablePlaceModel>
    fun searchByAliasStartingWith(searchedAlias: String): List<LocalizablePlaceModel>
    fun searchNearestLocalizablePlaces(localizacao: Point, raioEmKm: Double): List<LocalizablePlaceModel>
}
