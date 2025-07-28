package de.org.dexterity.bookanything.dom02assetmanager.domain.events

data class AssetRegisteredEvent(
    val assetId: Long,
    val tempFilePath: String,
    val parentAliasToAttach: String,
    val forceReimportIfExists: Boolean
)
