package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import java.util.*

interface LocalizablePlacePersistRepositoryPort {
    fun salvar(localizablePlaceModel: LocalizablePlaceModel): LocalizablePlaceModel
    fun deletarPorId(id: UUID)
    fun findAllForSync(): List<LocalizablePlaceModel>
    fun deletarTodos(): List<UUID>
    fun existsByName(name: String): Boolean
}