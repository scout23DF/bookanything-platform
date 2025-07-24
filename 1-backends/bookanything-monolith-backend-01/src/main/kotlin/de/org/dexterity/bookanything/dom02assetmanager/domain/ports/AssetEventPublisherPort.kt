package de.org.dexterity.bookanything.dom02assetmanager.domain.ports

import de.org.dexterity.bookanything.dom02assetmanager.domain.events.AssetUploadedEvent

interface AssetEventPublisherPort {
    suspend fun publishAssetUploadedEvent(event: AssetUploadedEvent)
}
