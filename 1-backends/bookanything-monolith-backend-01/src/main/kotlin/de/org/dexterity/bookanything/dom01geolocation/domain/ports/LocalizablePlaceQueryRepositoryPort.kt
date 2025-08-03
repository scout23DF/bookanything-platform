package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import org.locationtech.jts.geom.Point
import java.util.UUID

interface LocalizablePlaceQueryRepositoryPort {

    fun searchById(id: UUID): LocalizablePlaceModel?
    fun searchAll(): List<LocalizablePlaceModel>
    fun searchByAliasStartingWith(alias: String): List<LocalizablePlaceModel>
    fun searchByNearest(locationPointToSearch: Point, radiusInKm: Double): List<LocalizablePlaceModel>
    fun synchronizeFromWriteRepository(sourceLocalizablePlacesList: List<LocalizablePlaceModel>?): Map<String, Int>
    fun removeAll()

    fun searchByFriendlyIdContaining(friendlyId: String): List<LocalizablePlaceModel>
    fun searchByPropertiesDetailsMapContains(key: String, value: String): List<LocalizablePlaceModel>
    fun searchByLocationAsGeoHash(geoHashToSearch: String): List<LocalizablePlaceModel>

}