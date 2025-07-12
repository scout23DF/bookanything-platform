package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models

import org.locationtech.jts.geom.Point
import java.util.UUID

data class CentroDistribuicaoModel(
    val id: UUID,
    val nome: String,
    val localizacao: Point
)