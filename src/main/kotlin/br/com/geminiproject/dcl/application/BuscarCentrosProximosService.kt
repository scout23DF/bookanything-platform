package br.com.geminiproject.dcl.application

import br.com.geminiproject.dcl.domain.BuscarCentrosProximosUseCase
import br.com.geminiproject.dcl.domain.CentroDistribuicao
import br.com.geminiproject.dcl.domain.CentroDistribuicaoQueryRepositoryPort
import org.locationtech.jts.geom.Point

class BuscarCentrosProximosService(
    private val centroDistribuicaoQueryRepositoryPort: CentroDistribuicaoQueryRepositoryPort
) : BuscarCentrosProximosUseCase {

    override fun buscarCentrosProximos(localizacao: Point, raioEmKm: Double): List<CentroDistribuicao> {
        return centroDistribuicaoQueryRepositoryPort.buscarCentrosProximos(localizacao, raioEmKm)
    }
}
