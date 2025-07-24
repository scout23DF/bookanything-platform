package de.org.dexterity.bookanything.dom02assetmanager.application.mappers

import de.org.dexterity.bookanything.dom02assetmanager.domain.dtos.AssetDto
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.shared.annotations.Mapper

@Mapper
class AssetMapper {

    fun toDto(asset: AssetModel): AssetDto {
        return AssetDto(
            id = asset.id!!,
            fileName = asset.fileName,
            mimeType = asset.mimeType,
            size = asset.size,
            category = asset.category,
            status = asset.status,
            metadataMap = asset.metadataMap,
            bucket = asset.bucket.name,
            createdAt = asset.createdAt,
            updatedAt = asset.updatedAt
        )
    }
}
