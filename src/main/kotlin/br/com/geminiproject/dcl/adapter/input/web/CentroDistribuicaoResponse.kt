package br.com.geminiproject.dcl.adapter.input.web

import br.com.geminiproject.dcl.domain.CentroDistribuicao
import java.util.UUID

data class CentroDistribuicaoResponse(
    val id: UUID,
    val nome: String,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        fun fromDomain(domain: CentroDistribuicao): CentroDistribuicaoResponse {
            return CentroDistribuicaoResponse(
                id = domain.id,
                nome = domain.nome,
                latitude = domain.localizacao.y,
                longitude = domain.localizacao.x
            )
        }
    }
}
