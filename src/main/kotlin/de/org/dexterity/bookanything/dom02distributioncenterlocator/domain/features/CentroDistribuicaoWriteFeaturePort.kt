package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.features

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models.CentroDistribuicaoModel
import org.locationtech.jts.geom.Point
import java.util.*

interface CentroDistribuicaoWriteFeaturePort {
    fun cadastrar(nome: String, localizacao: Point): CentroDistribuicaoModel
    fun deletarPorId(id: UUID)
    fun synchronizeAll(): Map<String, Int>
    fun deletarTodos()
}
