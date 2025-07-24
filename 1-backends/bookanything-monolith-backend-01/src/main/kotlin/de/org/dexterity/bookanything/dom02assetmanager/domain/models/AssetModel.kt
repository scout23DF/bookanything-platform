package de.org.dexterity.bookanything.dom02assetmanager.domain.models

import java.time.Instant

data class AssetModel(
    val id: Long? = null,
    val bucket: BucketModel,
    var fileName: String,
    val storageKey: String,
    val mimeType: String,
    val size: Long,
    val category: AssetCategory,
    var metadataMap: Map<String, Any> = mutableMapOf(),
    var status: AssetStatus,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)
