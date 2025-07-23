package de.org.dexterity.bookanything.dom02assetmanager.domain.models

import java.time.Instant

data class AssetModel(
    val id: Long? = null,
    val bucket: BucketModel,
    val fileName: String,
    val storageKey: String,
    val mimeType: String,
    val size: Long,
    val category: AssetCategory,
    val metadata: Map<String, Any> = mutableMapOf(),
    var status: AssetStatus,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)
