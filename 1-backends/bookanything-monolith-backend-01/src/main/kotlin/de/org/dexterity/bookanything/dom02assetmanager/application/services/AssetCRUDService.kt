package de.org.dexterity.bookanything.dom02assetmanager.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos.GenericAssetUploadRequestDto
import de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos.GenericUploadedAssetResponseDto
import de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos.UpdateAssetDto
import de.org.dexterity.bookanything.dom02assetmanager.domain.events.AssetRegisteredEvent
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.*
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.BucketPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.*

@Service
class AssetCRUDService(
    private val assetRepository: AssetPersistRepositoryPort,
    private val bucketRepository: BucketPersistRepositoryPort,
    private val storageProvider: StorageProviderPort,
    private val eventPublisher: EventPublisherPort
) {

    @Transactional
    suspend fun uploadAssetFromMultipartFile(
        uploadedMultipartFile: MultipartFile,
        bucketName: String?,
        category: AssetCategory,
        metadata: Map<String, Any>,
        parentAliasToAttach: String,
        forceReimportIfExists: Boolean
    ): AssetModel {

        val genericAssetUploadRequestDto = GenericAssetUploadRequestDto(
            bucketName = bucketName,
            category = category,
            fileName = uploadedMultipartFile.originalFilename ?: "unknown-file",
            filesize = uploadedMultipartFile.size,
            contentType = uploadedMultipartFile.contentType ?: "application/octet-stream",
            fileContentAsBytes = uploadedMultipartFile.bytes,
            metadataMap = metadata
        )

        return uploadGenericAsset(genericAssetUploadRequestDto, parentAliasToAttach, forceReimportIfExists).createdAsset

    }

    @Transactional
    fun updateAsset(id: Long, dto: UpdateAssetDto): AssetModel? {
        val asset = assetRepository.findById(id).orElse(null) ?: return null
        
        dto.fileName?.let { asset.fileName = it }
        dto.metadataMap?.let { asset.metadataMap = it }

        return assetRepository.save(asset)
    }

    @Transactional
    suspend fun deleteAsset(id: Long): Boolean {
        val asset = assetRepository.findById(id).orElse(null) ?: return false

        storageProvider.delete(asset.bucket.name, asset.storageKey)
        assetRepository.delete(asset)
        
        return true
    }

    @Transactional(readOnly = true)
    fun findAllAssets(pageable: Pageable): Page<AssetModel> {
        return assetRepository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    fun findAssetById(id: Long): AssetModel? {
        return assetRepository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    fun findByFileNameStartingWith(fileName: String, pageable: Pageable): Page<AssetModel> {
        return assetRepository.findByFileNameStartingWith(fileName, pageable)
    }

    @Transactional(readOnly = true)
    fun findByBucketName(bucketName: String, pageable: Pageable): Page<AssetModel> {
        return assetRepository.findByBucketName(bucketName, pageable)
    }

    @Transactional(readOnly = true)
    fun findByStorageKeyStartingWith(storageKey: String, pageable: Pageable): Page<AssetModel> {
        return assetRepository.findByStorageKeyStartingWith(storageKey, pageable)
    }

    @Transactional(readOnly = true)
    fun findByCategory(category: AssetCategory, pageable: Pageable): Page<AssetModel> {
        return assetRepository.findByCategory(category, pageable)
    }

    @Transactional(readOnly = true)
    fun findByMetadataContains(key: String, value: String, pageable: Pageable): Page<AssetModel> {
        return assetRepository.findByMetadataContains(key, value, pageable)
    }

    suspend fun uploadGenericAsset(
        assetUploadRequestDto: GenericAssetUploadRequestDto,
        parentAliasToAttach: String,
        forceReimportIfExists: Boolean
    ): GenericUploadedAssetResponseDto {

        val targetBucketName = assetUploadRequestDto.bucketName ?: inferBucketName(assetUploadRequestDto.category)

        storageProvider.createBucketIfNotExists(targetBucketName)

        val bucket = bucketRepository.findByName(targetBucketName).orElseGet {
            bucketRepository.save(BucketModel(name = targetBucketName, provider = StorageProviderType.MINIO)) // Assuming Minio for now
        }

        val originalFileName = assetUploadRequestDto.fileName
        val storageKey = generateStorageKey(assetUploadRequestDto.category, originalFileName)

        var asset = AssetModel(
            bucket = bucket,
            fileName = originalFileName,
            storageKey = storageKey,
            mimeType = assetUploadRequestDto.contentType,
            size = assetUploadRequestDto.filesize,
            category = assetUploadRequestDto.category,
            metadataMap = assetUploadRequestDto.metadataMap,
            status = AssetStatus.PROCESSING
        )
        asset = assetRepository.save(asset)

        val tempDir = File(System.getProperty("java.io.tmpdir"), "bookanything-assets")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        val tempFile = File(tempDir, "${asset.id}-${originalFileName}")
        tempFile.writeBytes(assetUploadRequestDto.fileContentAsBytes)

        eventPublisher.publish(
            AssetRegisteredEvent(
                assetId = asset.id!!,
                tempFile.absolutePath,
                parentAliasToAttach = parentAliasToAttach,
                forceReimportIfExists = forceReimportIfExists
            )
        )

        return GenericUploadedAssetResponseDto(
            createdAsset = asset,
            status = AssetStatus.PROCESSING,
            message = "Asset upload processed. Final status: ${asset.status}",
            absolutePathTempFile = tempFile.absolutePath
        )
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
