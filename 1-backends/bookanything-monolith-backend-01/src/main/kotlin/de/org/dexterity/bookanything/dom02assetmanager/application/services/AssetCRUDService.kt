package de.org.dexterity.bookanything.dom02assetmanager.application.services

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetStatus
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.BucketModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.StorageProviderType
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.BucketPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class AssetCRUDService(
    private val assetRepository: AssetPersistRepositoryPort,
    private val bucketRepository: BucketPersistRepositoryPort,
    private val storageProvider: StorageProviderPort
) {

    @Transactional
    suspend fun uploadAsset(
        file: MultipartFile,
        bucketName: String?,
        category: AssetCategory,
        metadata: Map<String, Any>
    ): AssetModel {
        val targetBucketName = bucketName ?: inferBucketName(category)
        storageProvider.createBucketIfNotExists(targetBucketName)

        val bucket = bucketRepository.findByName(targetBucketName).orElseGet {
            bucketRepository.save(BucketModel(name = targetBucketName, provider = StorageProviderType.MINIO)) // Assuming Minio for now
        }

        val originalFileName = file.originalFilename ?: "unknown-file"
        val storageKey = generateStorageKey(category, originalFileName)

        var asset = AssetModel(
            bucket = bucket,
            fileName = originalFileName,
            storageKey = storageKey,
            mimeType = file.contentType ?: "application/octet-stream",
            size = file.size,
            category = category,
            metadata = metadata,
            status = AssetStatus.UPLOADING
        )
        asset = assetRepository.save(asset)

        // TODO: Replace direct call with Kafka event publishing
        try {
            storageProvider.upload(targetBucketName, storageKey, file.inputStream, file.size, file.contentType!!)
            asset.status = AssetStatus.AVAILABLE
            assetRepository.save(asset)
        } catch (e: Exception) {
            asset.status = AssetStatus.ERROR
            assetRepository.save(asset)
            throw e
        }
        
        return asset
    }

    @Transactional(readOnly = true)
    fun findAssetById(id: Long): AssetModel? {
        return assetRepository.findById(id).orElse(null)
    }

    private fun inferBucketName(category: AssetCategory): String {
        return when (category) {
            AssetCategory.IMAGE -> "bookanything-images"
            AssetCategory.VIDEO -> "bookanything-videos"
            AssetCategory.DOCUMENT -> "bookanything-documents"
            else -> "bookanything-default"
        }
    }

    private fun generateStorageKey(category: AssetCategory, fileName: String): String {
        val uuid = UUID.randomUUID().toString()
        return "${category.name.lowercase()}/$uuid-$fileName"
    }
}
