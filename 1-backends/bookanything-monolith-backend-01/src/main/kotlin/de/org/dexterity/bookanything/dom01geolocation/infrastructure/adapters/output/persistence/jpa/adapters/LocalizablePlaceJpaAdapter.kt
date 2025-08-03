package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.LocalizablePlacePersistRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.LocalizablePlaceJpaMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.LocalizablePlaceJpaRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class LocalizablePlaceJpaAdapter(
    private val jpaRepository: LocalizablePlaceJpaRepository,
    private val jpaMapper: LocalizablePlaceJpaMapper
) : LocalizablePlacePersistRepositoryPort {

    override fun salvar(localizablePlaceModel: LocalizablePlaceModel): LocalizablePlaceModel {
        val entity = jpaMapper.toJpaEntity(localizablePlaceModel)
        val savedEntity = jpaRepository.save(entity)
        return jpaMapper.toDomainModel(savedEntity)
    }

    override fun deletarPorId(id: UUID) {
        jpaRepository.deleteById(id)
    }

    override fun findAllForSync(): List<LocalizablePlaceModel> {
        return jpaRepository.findAll().map { entity ->
            jpaMapper.toDomainModel(entity)
        }
    }

    override fun deletarTodos(): List<UUID> {
        val allIds = jpaRepository.findAll().map { it.id!! }
        jpaRepository.deleteAll()
        return allIds
    }

    override fun existsByName(name: String): Boolean {
        return jpaRepository.existsByName(name)
    }
}