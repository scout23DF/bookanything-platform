package de.org.dexterity.bookanything.dom02assetmanager.domain.models

import java.time.Instant

data class BucketModel(
    val id: Long? = null,
    val name: String,
    val provider: StorageProviderType,
    val createdAt: Instant = Instant.now()
)
