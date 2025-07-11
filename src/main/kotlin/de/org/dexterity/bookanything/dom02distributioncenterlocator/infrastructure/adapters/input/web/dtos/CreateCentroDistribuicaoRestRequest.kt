package de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.input.web.dtos

data class CreateCentroDistribuicaoRestRequest(
    val nome: String,
    val latitude: Double,
    val longitude: Double
)