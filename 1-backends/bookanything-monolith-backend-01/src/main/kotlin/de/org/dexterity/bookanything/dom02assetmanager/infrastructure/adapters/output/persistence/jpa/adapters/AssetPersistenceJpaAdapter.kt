package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.mappers.AssetJpaMapper
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.repositories.AssetJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Adapter
class AssetPersistenceJpaAdapter(
    val assetJpaRepository: AssetJpaRepository,
    val assetJpaMapper: AssetJpaMapper
) : AssetPersistRepositoryPort {

    @Transactional
    override fun save(asset: AssetModel): AssetModel {
        val entity = assetJpaMapper.toEntity(asset)
        val savedEntity = assetJpaRepository.save(entity)
        return assetJpaMapper.toDomain(savedEntity)
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Optional<AssetModel> {
        return assetJpaRepository.findById(id).map(assetJpaMapper::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<AssetModel> {
        return assetJpaRepository.findAll(pageable).map(assetJpaMapper::toDomain)
    }

    override fun delete(targetAssetModel: AssetModel) {
        assetJpaRepository.findById(targetAssetModel.id!!).let { assetJpaRepository.delete(it.get()) }
    }

    @Transactional(readOnly = true)
    override fun findByFileNameStartingWith(fileName: String, pageable: Pageable): Page<AssetModel> {
        return assetJpaRepository.findByFileNameStartingWith(fileName, pageable).map(assetJpaMapper::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findByBucketName(bucketName: String, pageable: Pageable): Page<AssetModel> {
        return assetJpaRepository.findByBucketName(bucketName, pageable).map(assetJpaMapper::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findByStorageKeyStartingWith(storageKey: String, pageable: Pageable): Page<AssetModel> {
        return assetJpaRepository.findByStorageKeyStartingWith(storageKey, pageable).map(assetJpaMapper::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findByCategory(category: AssetCategory, pageable: Pageable): Page<AssetModel> {
        return assetJpaRepository.findByCategory(category, pageable).map(assetJpaMapper::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findByMetadataContains(key: String, value: String, pageable: Pageable): Page<AssetModel> {
        return assetJpaRepository.findByMetadataContains(key, value, pageable).map(assetJpaMapper::toDomain)
    }
}