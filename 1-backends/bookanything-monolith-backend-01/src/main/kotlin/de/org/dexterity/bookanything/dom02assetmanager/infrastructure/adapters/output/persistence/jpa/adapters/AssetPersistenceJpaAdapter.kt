package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.mappers.AssetJpaMapper
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.repositories.AssetJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Adapter
class AssetPersistenceJpaAdapter(
    val assetRepository: AssetJpaRepository,
    val mapper: AssetJpaMapper
) : AssetPersistRepositoryPort {

    @Transactional
    override fun save(asset: AssetModel): AssetModel {
        val entity = mapper.toEntity(asset)
        val savedEntity = assetRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Optional<AssetModel> {
        return assetRepository.findById(id).map(mapper::toDomain)
    }
}