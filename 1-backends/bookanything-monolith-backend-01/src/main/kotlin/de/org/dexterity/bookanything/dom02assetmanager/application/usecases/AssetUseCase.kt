package de.org.dexterity.bookanything.dom02assetmanager.application.usecases

import de.org.dexterity.bookanything.dom02assetmanager.application.mappers.AssetMapper
import de.org.dexterity.bookanything.dom02assetmanager.application.services.AssetCRUDService
import de.org.dexterity.bookanything.dom02assetmanager.domain.dtos.AssetDto
import de.org.dexterity.bookanything.dom02assetmanager.domain.dtos.UploadAssetResponseDto
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
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
        val asset = assetCRUDService.uploadAsset(file, bucketName, category, metadata)
        return UploadAssetResponseDto(
            assetId = asset.id!!,
            status = asset.status,
            message = "Asset upload processed. Final status: ${asset.status}"
        )
    }

    fun handleFindById(id: Long): AssetDto? {
        return assetCRUDService.findAssetById(id)?.let { assetMapper.toDto(it) }
    }
}
