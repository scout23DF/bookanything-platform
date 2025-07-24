package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.input.messaging.kafka

import de.org.dexterity.bookanything.dom02assetmanager.domain.events.AssetUploadedEvent
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetStatus
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.io.File

@Component
class AssetKafkaConsumer(
    private val assetRepository: AssetPersistRepositoryPort,
    private val storageProvider: StorageProviderPort
) {

    @KafkaListener(topics = ["asset-uploads"], groupId = "asset-upload-processor")
    suspend fun consumeAssetUploadedEvent(event: AssetUploadedEvent) {
        val asset = assetRepository.findById(event.assetId).orElse(null)
        if (asset == null) {
            // Asset not found, maybe it was deleted
            return
        }

        val tempFile = File(event.tempFilePath)
        if (!tempFile.exists()) {
            asset.status = AssetStatus.ERROR
            assetRepository.save(asset)
            return
        }

        try {
            storageProvider.upload(
                asset.bucket.name,
                asset.storageKey,
                tempFile.inputStream(),
                asset.size,
                asset.mimeType
            )
            asset.status = AssetStatus.AVAILABLE
            assetRepository.save(asset)
        } catch (e: Exception) {
            asset.status = AssetStatus.ERROR
            assetRepository.save(asset)
        } finally {
            tempFile.delete()
        }
    }
}
