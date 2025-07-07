package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.jpa

import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.CentroDistribuicaoModel
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports.CentroDistribuicaoPersistRepositoryPort
import org.springframework.stereotype.Component
import java.util.*

@Component
class CentroDistribuicaoJpaAdapter(
    private val repository: CentroDistribuicaoJpaRepository
) : CentroDistribuicaoPersistRepositoryPort {

    override fun salvar(centroDistribuicaoModel: CentroDistribuicaoModel): CentroDistribuicaoModel {
        val entity = CentroDistribuicaoJpaEntity(
            id = centroDistribuicaoModel.id,
            nome = centroDistribuicaoModel.nome,
            localizacao = centroDistribuicaoModel.localizacao
        )
        val savedEntity = repository.save(entity)
        return CentroDistribuicaoModel(
            id = savedEntity.id!!,
            nome = savedEntity.nome!!,
            localizacao = savedEntity.localizacao!!
        )
    }

    override fun deletarPorId(id: UUID) {
        repository.deleteById(id)
    }

    override fun findAllForSync(): List<CentroDistribuicaoModel> {
        return repository.findAll().map { entity ->
            CentroDistribuicaoModel(
                id = entity.id!!,
                nome = entity.nome!!,
                localizacao = entity.localizacao!!
            )
        }
    }

    override fun deletarTodos(): List<UUID> {
        val allIds = repository.findAll().map { it.id!! }
        repository.deleteAll()
        return allIds
    }

    override fun existsByName(name: String): Boolean {
        return repository.existsByNome(name)
    }
}
