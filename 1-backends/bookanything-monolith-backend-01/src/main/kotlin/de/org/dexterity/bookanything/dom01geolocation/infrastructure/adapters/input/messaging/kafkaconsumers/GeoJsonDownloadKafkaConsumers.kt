package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.domain.events.CountryGeoJsonDataRequiredEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoJsonDownloadFailedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoJsonDownloadRequestedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.events.GeoJsonFileDownloadedEvent
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.geojson_providers.GadmGeoJsonProviderAdapter
import de.org.dexterity.bookanything.dom02assetmanager.application.services.dtos.GenericAssetUploadRequestDto
import de.org.dexterity.bookanything.dom02assetmanager.application.usecases.AssetUseCase
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths

@Component
class GeoJsonDownloadKafkaConsumers(
    private val eventPublisher: EventPublisherPort,
    private val geoJsonProvider: GadmGeoJsonProviderAdapter,
    private val assetUseCase: AssetUseCase
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${topics.geolocation.geojson-download.requested}"],
        groupId = "geolocation-downloader-dispatcher"
    )
    fun handleDownloadRequest(event: GeoJsonDownloadRequestedEvent) {
        logger.info("Received GeoJSON download request for job ${event.jobId}. Dispatching individual country download tasks...")

        event.request.countryIso3Codes.forEach { countryCode ->
            event.request.levels.forEach { level ->
                val countryEvent = CountryGeoJsonDataRequiredEvent(
                    jobId = event.jobId,
                    countryIso3Code = countryCode,
                    level = level
                )
                logger.debug("Publishing event to download data for $countryCode, level $level.")
                eventPublisher.publish(countryEvent)
            }
        }

        logger.info("All download tasks for job ${event.jobId} have been dispatched.")
    }

    @KafkaListener(
        topics = ["\${topics.geolocation.geojson-download.country-data-required}"],
        groupId = "geolocation-downloader-worker"
    )
    fun handleCountryDataRequired(event: CountryGeoJsonDataRequiredEvent) = runBlocking {
        val (jobId, countryCode, level) = event
        logger.info("Received request to download GeoJSON for $countryCode, level $level (Job ID: $jobId)")

        try {
            if (!geoJsonProvider.fileExists(countryCode, level)) {
                throw FileNotFoundException("File not found for $countryCode at level $level.")
            }

            val tempPath = geoJsonProvider.downloadFile(countryCode, level)
            val fileName = geoJsonProvider.getFileName(countryCode, level)

            val successEvent = GeoJsonFileDownloadedEvent(
                jobId = jobId,
                countryIso3Code = countryCode,
                level = level,
                tempFilePath = tempPath.toString(),
                fileName = fileName
            )
            eventPublisher.publish(successEvent)
            logger.info("Successfully downloaded and published success event for $countryCode, level $level.")

        } catch (e: Exception) {
            logger.error("Failed to download GeoJSON for $countryCode, level $level (Job ID: $jobId). Reason: ${e.message}")
            val failureEvent = GeoJsonDownloadFailedEvent(
                jobId = jobId,
                countryIso3Code = countryCode,
                level = level,
                reason = e.message ?: "Unknown error"
            )
            eventPublisher.publish(failureEvent)
        }
    }

    @KafkaListener(
        topics = ["\${topics.geolocation.geojson-file.downloaded}"],
        groupId = "geolocation-uploader-worker"
    )
    suspend fun handleFileDownloaded(event: GeoJsonFileDownloadedEvent) {
        val (jobId, countryCode, level, tempFilePathStr, fileName) = event
        logger.info("Received downloaded file event for $countryCode, level $level. Uploading to asset manager... (Job ID: $jobId)")
        val tempPath = Paths.get(tempFilePathStr)

        try {
            val fileBytes = Files.readAllBytes(tempPath)
            val assetUploadRequestDto = GenericAssetUploadRequestDto(
                bucketName = "geojson-imports", // Or make this configurable
                fileName = fileName,
                contentType = "application/json",
                category = AssetCategory.DATA_GEOJSON,
                filesize = fileBytes.size.toLong(),
                fileContentAsBytes = fileBytes,
                metadataMap = mapOf(
                    "jobId" to jobId.toString(),
                    "country" to countryCode,
                    "level" to level.toString(),
                    "source" to "GADMv4.1"
                )
            )

            val uploadAssetResponseDto = assetUseCase.uploadGenericAsset(assetUploadRequestDto)

            logger.info("Successfully uploaded $fileName to the StorageProvider via AssetManager :: Response: $uploadAssetResponseDto ")

        } catch (e: Exception) {
            logger.error("Failed to upload $fileName for job $jobId. Reason: ${e.message}")
            val failureEvent = GeoJsonDownloadFailedEvent(
                jobId = jobId,
                countryIso3Code = countryCode,
                level = level,
                reason = "Failed to upload to asset manager: ${e.message}"
            )
            eventPublisher.publish(failureEvent)
        } finally {
            try {
                Files.deleteIfExists(tempPath)
                logger.debug("Deleted temporary file: {}", tempPath)
            } catch (e: Exception) {
                logger.error("Failed to delete temporary file $tempPath. Please clean up manually.", e)
            }
        }
    }
}