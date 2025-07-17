package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.LocalizablePlacePersistRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.LocalizablePlaceJpaEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.LocalizablePlaceJpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class LocalizablePlaceJpaAdapter(
    private val repository: LocalizablePlaceJpaRepository
) : LocalizablePlacePersistRepositoryPort {

    override fun salvar(localizablePlaceModel: LocalizablePlaceModel): LocalizablePlaceModel {
        val entity = LocalizablePlaceJpaEntity(
            id = localizablePlaceModel.id,
            name = localizablePlaceModel.name,
            alias = localizablePlaceModel.alias,
            locationPoint = localizablePlaceModel.locationPoint
        )
        val savedEntity = repository.save(entity)
        return LocalizablePlaceModel(
            id = savedEntity.id!!,
            name = savedEntity.name!!,
            alias = savedEntity.alias,
            locationPoint = savedEntity.locationPoint!!
        )
    }

    override fun deletarPorId(id: UUID) {
        repository.deleteById(id)
    }

    override fun findAllForSync(): List<LocalizablePlaceModel> {
        return repository.findAll().map { entity ->
            LocalizablePlaceModel(
                id = entity.id!!,
                name = entity.name!!,
            alias = entity.alias,
            locationPoint = entity.locationPoint!!
            )
        }
    }

    override fun deletarTodos(): List<UUID> {
        val allIds = repository.findAll().map { it.id!! }
        repository.deleteAll()
        return allIds
    }

    override fun existsByName(name: String): Boolean {
        return repository.existsByName(name)
    }
}