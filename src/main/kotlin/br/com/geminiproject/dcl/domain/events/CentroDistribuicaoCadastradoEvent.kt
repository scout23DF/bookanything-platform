package br.com.geminiproject.dcl.domain.events

import java.time.LocalDateTime
import java.util.UUID

data class CentroDistribuicaoCadastradoEvent(
    val id: UUID,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
