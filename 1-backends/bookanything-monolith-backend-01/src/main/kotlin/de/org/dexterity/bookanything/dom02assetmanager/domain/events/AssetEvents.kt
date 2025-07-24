package de.org.dexterity.bookanything.dom02assetmanager.domain.events

data class AssetUploadedEvent(
    val assetId: Long,
    val tempFilePath: String
)
