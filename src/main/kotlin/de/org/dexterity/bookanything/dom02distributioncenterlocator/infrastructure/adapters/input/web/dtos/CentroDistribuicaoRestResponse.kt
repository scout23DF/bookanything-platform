package de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.dtos

import java.util.*

data class CentroDistribuicaoRestResponse(
    val id: UUID,
    val nome: String,
    val latitude: Double,
    val longitude: Double
)