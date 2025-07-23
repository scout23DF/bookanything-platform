package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.BucketModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.BucketPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.mappers.AssetJpaMapper
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.repositories.BucketJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Adapter
class BucketPersistenceJpaAdapter(
    val bucketRepository: BucketJpaRepository,
    val mapper: AssetJpaMapper
) : BucketPersistRepositoryPort {

    @Transactional(readOnly = true)
    override fun findByName(name: String): Optional<BucketModel> {
        return bucketRepository.findByName(name).map(mapper::toDomain)
    }

    @Transactional
    override fun save(bucket: BucketModel): BucketModel {
        val entity = mapper.toEntity(bucket)
        val savedEntity = bucketRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }
}