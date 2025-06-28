package br.com.geminiproject.dcl.domain

import org.locationtech.jts.geom.Point
import java.util.UUID

data class CentroDistribuicao(
    val id: UUID,
    val nome: String,
    val localizacao: Point
)
