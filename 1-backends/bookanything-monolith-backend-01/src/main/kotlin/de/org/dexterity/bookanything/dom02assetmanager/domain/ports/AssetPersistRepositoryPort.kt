package de.org.dexterity.bookanything.dom02assetmanager.domain.ports

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface AssetPersistRepositoryPort {
    fun save(asset: AssetModel): AssetModel
    fun findById(id: Long): Optional<AssetModel>
    fun findAll(pageable: Pageable): Page<AssetModel>
    fun findByFileNameStartingWith(fileName: String, pageable: Pageable): Page<AssetModel>
    fun findByBucketName(bucketName: String, pageable: Pageable): Page<AssetModel>
    fun findByStorageKeyStartingWith(storageKey: String, pageable: Pageable): Page<AssetModel>
    fun findByCategory(category: AssetCategory, pageable: Pageable): Page<AssetModel>
    fun findByMetadataContains(key: String, value: String, pageable: Pageable): Page<AssetModel>
    fun delete(targetAssetModel: AssetModel)
}
