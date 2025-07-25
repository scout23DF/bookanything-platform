package de.org.dexterity.bookanything.dom01geolocation.domain.features

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import org.locationtech.jts.geom.Point
import java.util.*

interface LocalizablePlaceWriteFeaturePort {
    fun cadastrar(nome: String, alias: String?, friendlyId: String, propertiesDetailsMap: Map<String, Any>?, localizacao: Point): LocalizablePlaceModel
    fun deletarPorId(id: UUID)
    fun synchronizeAll(): Map<String, Int>
    fun deletarTodos()
}
