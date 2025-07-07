package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.input.web

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.CentroDistribuicaoModel
import java.util.UUID

data class CentroDistribuicaoResponse(
    val id: UUID,
    val nome: String,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        fun fromDomain(domain: CentroDistribuicaoModel): CentroDistribuicaoResponse {
            return CentroDistribuicaoResponse(
                id = domain.id,
                nome = domain.nome,
                latitude = domain.localizacao.y,
                longitude = domain.localizacao.x
            )
        }
    }
}
