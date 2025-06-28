package br.com.geminiproject.dcl.domain

import org.locationtech.jts.geom.Point
import java.util.UUID

interface CentroDistribuicaoQueryRepositoryPort {
    fun buscarPorId(id: UUID): CentroDistribuicao?
    fun buscarTodos(): List<CentroDistribuicao>
    fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<CentroDistribuicao>
}
