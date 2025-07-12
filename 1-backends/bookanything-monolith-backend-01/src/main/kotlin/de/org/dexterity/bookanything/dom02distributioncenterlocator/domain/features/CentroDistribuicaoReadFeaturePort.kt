package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.features

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models.CentroDistribuicaoModel
import org.locationtech.jts.geom.Point
import java.util.UUID

interface CentroDistribuicaoReadFeaturePort {
    fun buscarPorId(id: UUID): CentroDistribuicaoModel?
    fun buscarTodos(): List<CentroDistribuicaoModel>
    fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<CentroDistribuicaoModel>
}
