package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import org.locationtech.jts.geom.Point
import java.util.UUID

interface LocalizablePlaceQueryRepositoryPort {

    fun buscarPorId(id: UUID): LocalizablePlaceModel?
    fun buscarTodos(): List<LocalizablePlaceModel>
    fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<LocalizablePlaceModel>
    fun synchronizeFromWriteRepository(sourceCentroDistribuicaoList: List<LocalizablePlaceModel>?): Map<String, Int>
    fun deletarTodos()

}