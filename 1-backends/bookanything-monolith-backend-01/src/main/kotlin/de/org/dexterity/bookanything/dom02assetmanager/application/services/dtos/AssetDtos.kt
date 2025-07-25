package de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
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

data class GenericAssetUploadRequestDto(
    val bucketName: String?,
    val category: AssetCategory,
    val fileName: String,
    val filesize: Long,
    val contentType: String,
    val fileContentAsBytes: ByteArray,
    val metadataMap: Map<String, Any>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GenericAssetUploadRequestDto

        if (bucketName != other.bucketName) return false
        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false
        if (!fileContentAsBytes.contentEquals(other.fileContentAsBytes)) return false
        if (metadataMap != other.metadataMap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bucketName.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + fileContentAsBytes.contentHashCode()
        result = 31 * result + metadataMap.hashCode()
        return result
    }
}

data class GenericUploadedAssetResponseDto(
    val createdAsset: AssetModel,
    val status: AssetStatus,
    val message: String? = null,
    val absolutePathTempFile: String? = null
)

