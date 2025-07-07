package de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.CentroDistribuicaoModel
import java.util.*

interface CentroDistribuicaoPersistRepositoryPort {
    fun salvar(centroDistribuicaoModel: CentroDistribuicaoModel): CentroDistribuicaoModel
    fun deletarPorId(id: UUID)
    fun findAllForSync(): List<CentroDistribuicaoModel>
    fun deletarTodos(): List<UUID>
    fun existsByName(name: String): Boolean
}