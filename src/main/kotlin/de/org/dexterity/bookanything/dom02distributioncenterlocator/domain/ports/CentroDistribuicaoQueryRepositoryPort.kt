package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models.CentroDistribuicaoModel
import org.locationtech.jts.geom.Point
import java.util.UUID

interface CentroDistribuicaoQueryRepositoryPort {

    fun buscarPorId(id: UUID): CentroDistribuicaoModel?
    fun buscarTodos(): List<CentroDistribuicaoModel>
    fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<CentroDistribuicaoModel>
    fun synchronizeFromWriteRepository(sourceCentroDistribuicaoList: List<CentroDistribuicaoModel>?): Map<String, Int>
    fun deletarTodos()

}