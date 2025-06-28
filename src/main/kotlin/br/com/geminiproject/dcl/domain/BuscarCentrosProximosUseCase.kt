package br.com.geminiproject.dcl.domain

import org.locationtech.jts.geom.Point

interface BuscarCentrosProximosUseCase {
    fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<CentroDistribuicao>
}
