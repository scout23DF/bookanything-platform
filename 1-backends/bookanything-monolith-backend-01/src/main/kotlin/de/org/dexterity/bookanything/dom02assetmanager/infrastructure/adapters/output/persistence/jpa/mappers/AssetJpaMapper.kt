package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.BucketModel
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.entities.AssetEntity
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.entities.BucketEntity
import de.org.dexterity.bookanything.shared.annotations.Mapper

@Mapper
class AssetJpaMapper {

    // Bucket Mappers
    fun toDomain(entity: BucketEntity): BucketModel = BucketModel(
        id = entity.id,
        name = entity.name,
        provider = entity.provider,
        createdAt = entity.createdAt
    )

    fun toEntity(domain: BucketModel): BucketEntity = BucketEntity(
        id = domain.id,
        name = domain.name,
        provider = domain.provider,
        createdAt = domain.createdAt
    )

    // Asset Mappers
    fun toDomain(entity: AssetEntity): AssetModel = AssetModel(
        id = entity.id,
        bucket = toDomain(entity.bucket),
        fileName = entity.fileName,
        storageKey = entity.storageKey,
        mimeType = entity.mimeType,
        size = entity.size,
        category = entity.category,
        metadata = entity.metadata,
        status = entity.status,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(domain: AssetModel): AssetEntity = AssetEntity(
        id = domain.id,
        bucket = toEntity(domain.bucket),
        fileName = domain.fileName,
        storageKey = domain.storageKey,
        mimeType = domain.mimeType,
        size = domain.size,
        category = domain.category,
        metadata = domain.metadata,
        status = domain.status,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}