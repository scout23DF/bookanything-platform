package br.com.geminiproject.dcl.domain.ports

import br.com.geminiproject.dcl.domain.CentroDistribuicaoModel
import org.locationtech.jts.geom.Point
import java.util.UUID

interface CentroDistribuicaoQueryRepositoryPort {
    fun buscarPorId(id: UUID): CentroDistribuicaoModel?
    fun buscarTodos(): List<CentroDistribuicaoModel>
    fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<CentroDistribuicaoModel>
    fun synchronizeFromWriteRepository(sourceCentroDistribuicaoList : List<CentroDistribuicaoModel>?): Map<String, Int>
}