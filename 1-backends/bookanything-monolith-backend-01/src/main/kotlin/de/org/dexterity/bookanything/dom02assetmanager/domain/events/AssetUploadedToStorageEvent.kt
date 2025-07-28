package de.org.dexterity.bookanything.dom02assetmanager.domain.events

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel

/**
 * Event triggered when a new asset is successfully created, uploaded to the storage,
 * and marked as AVAILABLE in the system.
 */
data class AssetUploadedToStorageEvent(
    val asset: AssetModel,
    val parentAliasToAttach: String,
    val forceReimportIfExists: Boolean
)
