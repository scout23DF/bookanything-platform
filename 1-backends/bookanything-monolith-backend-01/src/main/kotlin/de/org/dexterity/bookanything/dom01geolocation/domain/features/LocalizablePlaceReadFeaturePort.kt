package de.org.dexterity.bookanything.dom01geolocation.domain.features

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import org.locationtech.jts.geom.Point
import java.util.UUID

interface LocalizablePlaceReadFeaturePort {
    fun buscarPorId(id: UUID): LocalizablePlaceModel?
    fun buscarTodos(): List<LocalizablePlaceModel>
    fun buscarPorAliasIniciandoPor(searchedAlias: String): List<LocalizablePlaceModel>
    fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<LocalizablePlaceModel>
}
