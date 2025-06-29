package br.com.geminiproject.dcl.domain.features

import br.com.geminiproject.dcl.domain.CentroDistribuicaoModel
import org.locationtech.jts.geom.Point
import java.util.UUID

interface CentroDistribuicaoReadFeaturePort {
    fun buscarPorId(id: UUID): CentroDistribuicaoModel?
    fun buscarTodos(): List<CentroDistribuicaoModel>
    fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<CentroDistribuicaoModel>
}
