package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.*
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.BucketPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.time.Instant

@Service
class GeoLocationAssetIntegrationService(
    private val storageProvider: StorageProviderPort,
    private val assetRepository: AssetPersistRepositoryPort,
    private val bucketRepository: BucketPersistRepositoryPort
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val IMAGES_BUCKET_NAME = "bookanything-images"
        const val DOCUMENTS_BUCKET_NAME = "bookanything-documents"
        const val SVG_MIME_TYPE = "image/svg+xml"
        const val PDF_MIME_TYPE = "application/pdf"
    }

    /**
     * Stores both the local map and world highlight SVG images into the Tenant MinIO
     * using the Asset Management framework and registers them in the asset catalog.
     */
    suspend fun saveGeoLocationMapSvgs(
        geoLocation: IGeoLocationModel,
        localMapSvg: String,
        worldHighlightSvg: String
    ): Pair<AssetModel, AssetModel> {
        val geoId = geoLocation.id.id
        val friendlyId = geoLocation.friendlyId
        val type = geoLocation.type.name

        logger.info("Asset Management: Uploading SVG maps for GeoLocation #$geoId ($friendlyId) to Tenant MinIO '$IMAGES_BUCKET_NAME'...")

        // 1. Ensure target bucket exists in Tenant MinIO and database
        storageProvider.createBucketIfNotExists(IMAGES_BUCKET_NAME)
        val bucket = bucketRepository.findByName(IMAGES_BUCKET_NAME).orElseGet {
            bucketRepository.save(
                BucketModel(
                    name = IMAGES_BUCKET_NAME,
                    provider = StorageProviderType.MINIO
                )
            )
        }

        // 2. Upload Local Map SVG
        val localFileName = "map-$friendlyId.svg"
        val localStorageKey = "images/geolocations/$geoId/$localFileName"
        val localBytes = localMapSvg.toByteArray(Charsets.UTF_8)

        storageProvider.upload(
            bucketName = IMAGES_BUCKET_NAME,
            key = localStorageKey,
            inputStream = ByteArrayInputStream(localBytes),
            size = localBytes.size.toLong(),
            mimeType = SVG_MIME_TYPE
        )

        val localAsset = saveOrUpdateAsset(
            bucket = bucket,
            fileName = localFileName,
            storageKey = localStorageKey,
            mimeType = SVG_MIME_TYPE,
            size = localBytes.size.toLong(),
            category = AssetCategory.IMAGE,
            metadata = mapOf(
                "geoLocationId" to geoId,
                "friendlyId" to friendlyId,
                "type" to type,
                "kind" to "LOCAL_MAP",
                "uploadedAt" to Instant.now().toString()
            )
        )
        logger.info("Asset Management: Local Map SVG registered as Asset #${localAsset.id} (key: $localStorageKey)")

        // 3. Upload World Highlight Map SVG
        val worldFileName = "world-$friendlyId.svg"
        val worldStorageKey = "images/geolocations/$geoId/$worldFileName"
        val worldBytes = worldHighlightSvg.toByteArray(Charsets.UTF_8)

        storageProvider.upload(
            bucketName = IMAGES_BUCKET_NAME,
            key = worldStorageKey,
            inputStream = ByteArrayInputStream(worldBytes),
            size = worldBytes.size.toLong(),
            mimeType = SVG_MIME_TYPE
        )

        val worldAsset = saveOrUpdateAsset(
            bucket = bucket,
            fileName = worldFileName,
            storageKey = worldStorageKey,
            mimeType = SVG_MIME_TYPE,
            size = worldBytes.size.toLong(),
            category = AssetCategory.IMAGE,
            metadata = mapOf(
                "geoLocationId" to geoId,
                "friendlyId" to friendlyId,
                "type" to type,
                "kind" to "WORLD_HIGHLIGHT",
                "uploadedAt" to Instant.now().toString()
            )
        )
        logger.info("Asset Management: World Map SVG registered as Asset #${worldAsset.id} (key: $worldStorageKey)")

        return Pair(localAsset, worldAsset)
    }

    /**
     * Stores the generated executive detail report PDF into the Tenant MinIO
     * using the Asset Management framework under bookanything-documents bucket.
     */
    suspend fun saveGeoLocationReportPdf(
        geoLocation: IGeoLocationModel,
        pdfBytes: ByteArray
    ): AssetModel {
        val geoId = geoLocation.id.id
        val friendlyId = geoLocation.friendlyId
        val type = geoLocation.type.name

        logger.info("Asset Management: Uploading detail report PDF for GeoLocation #$geoId ($friendlyId) to Tenant MinIO '$DOCUMENTS_BUCKET_NAME'...")

        storageProvider.createBucketIfNotExists(DOCUMENTS_BUCKET_NAME)
        val bucket = bucketRepository.findByName(DOCUMENTS_BUCKET_NAME).orElseGet {
            bucketRepository.save(
                BucketModel(
                    name = DOCUMENTS_BUCKET_NAME,
                    provider = StorageProviderType.MINIO
                )
            )
        }

        val reportFileName = "report-$friendlyId.pdf"
        val reportStorageKey = "documents/geolocations/$geoId/$reportFileName"

        storageProvider.upload(
            bucketName = DOCUMENTS_BUCKET_NAME,
            key = reportStorageKey,
            inputStream = ByteArrayInputStream(pdfBytes),
            size = pdfBytes.size.toLong(),
            mimeType = PDF_MIME_TYPE
        )

        val reportAsset = saveOrUpdateAsset(
            bucket = bucket,
            fileName = reportFileName,
            storageKey = reportStorageKey,
            mimeType = PDF_MIME_TYPE,
            size = pdfBytes.size.toLong(),
            category = AssetCategory.DOCUMENT,
            metadata = mapOf(
                "geoLocationId" to geoId,
                "friendlyId" to friendlyId,
                "type" to type,
                "kind" to "GEOLOCATION_DETAIL_REPORT",
                "uploadedAt" to Instant.now().toString()
            )
        )
        logger.info("Asset Management: Detail Report PDF registered as Asset #${reportAsset.id} (key: $reportStorageKey)")
        return reportAsset
    }

    private fun saveOrUpdateAsset(
        bucket: BucketModel,
        fileName: String,
        storageKey: String,
        mimeType: String,
        size: Long,
        category: AssetCategory,
        metadata: Map<String, Any>
    ): AssetModel {
        val existing = assetRepository.findByStorageKeyStartingWith(storageKey, PageRequest.of(0, 1))
            .content.firstOrNull { it.storageKey == storageKey }

        return if (existing != null) {
            val updated = existing.copy(
                fileName = fileName,
                mimeType = mimeType,
                size = size,
                category = category,
                metadataMap = metadata,
                status = AssetStatus.AVAILABLE,
                updatedAt = Instant.now()
            )
            assetRepository.save(updated)
        } else {
            assetRepository.save(
                AssetModel(
                    bucket = bucket,
                    fileName = fileName,
                    storageKey = storageKey,
                    mimeType = mimeType,
                    size = size,
                    category = category,
                    metadataMap = metadata,
                    status = AssetStatus.AVAILABLE
                )
            )
        }
    }
}
