package de.org.dexterity.bookanything.dom02assetmanager.application.usecases

import de.org.dexterity.bookanything.dom02assetmanager.application.mappers.AssetMapper
import de.org.dexterity.bookanything.dom02assetmanager.application.services.AssetCRUDService
import de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos.*
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class AssetUseCase(
    private val assetCRUDService: AssetCRUDService,
    private val assetMapper: AssetMapper
) {

    suspend fun handleUpload(
        file: MultipartFile,
        bucketName: String?,
        category: AssetCategory,
        metadata: Map<String, Any>
    ): UploadAssetResponseDto {
        val asset = assetCRUDService.uploadAssetFromMultipartFile(file, bucketName, category, metadata)
        return UploadAssetResponseDto(
            assetId = asset.id!!,
            status = asset.status,
            message = "Asset upload processed. Final status: ${asset.status}"
        )
    }

    fun handleUpdate(id: Long, dto: UpdateAssetDto): AssetDto? {
        return assetCRUDService.updateAsset(id, dto)?.let { assetMapper.toDto(it) }
    }

    suspend fun handleDelete(id: Long): Boolean {
        return assetCRUDService.deleteAsset(id)
    }

    fun handleFindAll(pageable: Pageable): Page<AssetDto> {
        return assetCRUDService.findAllAssets(pageable).map { assetMapper.toDto(it) }
    }

    fun handleFindById(id: Long): AssetDto? {
        return assetCRUDService.findAssetById(id)?.let { assetMapper.toDto(it) }
    }

    fun handleFindByFileNameStartingWith(fileName: String, pageable: Pageable): Page<AssetDto> {
        return assetCRUDService.findByFileNameStartingWith(fileName, pageable).map { assetMapper.toDto(it) }
    }

    fun handleFindByBucketName(bucketName: String, pageable: Pageable): Page<AssetDto> {
        return assetCRUDService.findByBucketName(bucketName, pageable).map { assetMapper.toDto(it) }
    }

    fun handleFindByStorageKeyStartingWith(storageKey: String, pageable: Pageable): Page<AssetDto> {
        return assetCRUDService.findByStorageKeyStartingWith(storageKey, pageable).map { assetMapper.toDto(it) }
    }

    fun handleFindByCategory(category: AssetCategory, pageable: Pageable): Page<AssetDto> {
        return assetCRUDService.findByCategory(category, pageable).map { assetMapper.toDto(it) }
    }

    fun handleFindByMetadataContains(key: String, value: String, pageable: Pageable): Page<AssetDto> {
        return assetCRUDService.findByMetadataContains(key, value, pageable).map { assetMapper.toDto(it) }
    }

    suspend fun uploadGenericAsset(assetUploadRequestDto: GenericAssetUploadRequestDto): GenericUploadedAssetResponseDto {
        return assetCRUDService.uploadGenericAsset(assetUploadRequestDto)
    }


}

