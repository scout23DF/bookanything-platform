package de.org.dexterity.bookanything.dom02assetmanager.domain.dtos

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetStatus
import java.time.Instant

data class AssetDto(
    val id: Long,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val category: AssetCategory,
    val status: AssetStatus,
    val metadataMap: Map<String, Any>,
    val bucket: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class UploadAssetResponseDto(
    val assetId: Long,
    val status: AssetStatus,
    val message: String
)

data class UpdateAssetDto(
    val fileName: String?,
    val metadataMap: Map<String, Any>?
)
