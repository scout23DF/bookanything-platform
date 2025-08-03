package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.input.messaging.kafka

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonImporterUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.GeoJsonImportedFileJpaRepository
import de.org.dexterity.bookanything.dom02assetmanager.domain.events.AssetRegisteredEvent
import de.org.dexterity.bookanything.dom02assetmanager.domain.events.AssetUploadedToStorageEvent
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetStatus
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.io.File

@Component
class AssetKafkaConsumers(
    private val assetRepository: AssetPersistRepositoryPort,
    private val storageProvider: StorageProviderPort,
    private val eventPublisher: EventPublisherPort,
    private val geoJsonImportedFileJpaRepository: GeoJsonImportedFileJpaRepository,
    private val geoJsonImporterUseCase: GeoJsonImporterUseCase
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val GEOJSON_MIME_TYPE = "application/json"
    }


    @KafkaListener(
        topics = ["\${topics.asset-manager.asset-creation.registered}"],
        groupId = "assetmanager-pos-registered-asset-processor"
    )
    suspend fun consumeAssetRegisteredEvent(assetRegisteredEvent: AssetRegisteredEvent) {
        val asset = assetRepository.findById(assetRegisteredEvent.assetId).orElse(null)
        if (asset == null) {
            // Asset not found, maybe it was deleted
            return
        }

        val tempFile = File(assetRegisteredEvent.tempFilePath)
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
            val savedAsset = assetRepository.save(asset)

            // Publish the new event with the full asset model
            eventPublisher.publish(
                AssetUploadedToStorageEvent(
                    savedAsset,
                    assetRegisteredEvent.targetCountryCode,
                    assetRegisteredEvent.hierarchyDetailsRequest
                )
            )

        } catch (e: Exception) {
            asset.status = AssetStatus.ERROR
            assetRepository.save(asset)
        } finally {
            tempFile.delete()
        }
    }

    @KafkaListener(
        topics = ["\${topics.asset-manager.asset-creation.uploaded-to-storage}"],
        groupId = "assetmanager-pos-uploaded-asset-processor"
    )
    fun consumeAssetCreatedEvent(assetUploadedToStorageEvent: AssetUploadedToStorageEvent) {

        log.info("Received asset created event for asset ID: {}", assetUploadedToStorageEvent.asset.id)

        val registeredAsset = assetUploadedToStorageEvent.asset
        if (registeredAsset.mimeType != GEOJSON_MIME_TYPE) {
            log.debug("Asset ID {} is not a GeoJSON file (mimeType: {}). Skipping.", registeredAsset.id, registeredAsset.mimeType)
            return
        }

        if (geoJsonImportedFileJpaRepository.existsByFileName(registeredAsset.fileName)) {
            log.warn("GeoJSON file {} (Asset ID: {}) has already been imported. Skipping.", registeredAsset.fileName, registeredAsset.id)
            return
        }

        log.info("Asset ID {} is a new GeoJSON file. Starting import process for {}.", registeredAsset.id, registeredAsset.fileName)

        runBlocking {
            geoJsonImporterUseCase.execute(
                registeredAsset,
                assetUploadedToStorageEvent.targetCountryCode,
                assetUploadedToStorageEvent.hierarchyDetailsRequest
            )
        }
    }

}
