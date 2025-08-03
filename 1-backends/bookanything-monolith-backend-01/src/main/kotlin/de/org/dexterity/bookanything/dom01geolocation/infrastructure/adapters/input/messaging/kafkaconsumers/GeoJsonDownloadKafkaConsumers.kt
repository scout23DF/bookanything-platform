package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.messaging.kafkaconsumers

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.HierarchyDetailsRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.events.HierarchicalGeoJsonDataRequiredEvent
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

        event.geoJsonDownloadRequest.countryDataToImportRequestList.forEach { oneCountryToImportRequest ->

            if (oneCountryToImportRequest.importingDetailsForCountry != null) {

                val importRequestForCountryLevelEvent = HierarchicalGeoJsonDataRequiredEvent(
                    jobId = event.jobId,
                    countryIso3Code = oneCountryToImportRequest.countryIso3Code,
                    hierarchyDetailsRequest = oneCountryToImportRequest.importingDetailsForCountry
                )
                logger.debug("Publishing Event to download data for $oneCountryToImportRequest.countryIso3Code, Country Level, File Level No.: ${oneCountryToImportRequest.importingDetailsForCountry.hierarchyLevelOfFileToImport}.")
                eventPublisher.publish(importRequestForCountryLevelEvent)

            }

            if (oneCountryToImportRequest.importingDetailsForProvince != null) {

                val importRequestForProvinceLevelEvent = HierarchicalGeoJsonDataRequiredEvent(
                    jobId = event.jobId,
                    countryIso3Code = oneCountryToImportRequest.countryIso3Code,
                    hierarchyDetailsRequest = oneCountryToImportRequest.importingDetailsForProvince
                )
                logger.debug("Publishing Event to download data for $oneCountryToImportRequest.countryIso3Code, Province Level, File Level No.: ${oneCountryToImportRequest.importingDetailsForProvince.hierarchyLevelOfFileToImport}.")
                eventPublisher.publish(importRequestForProvinceLevelEvent)

            }

            if (oneCountryToImportRequest.importingDetailsForCity != null) {

                val importRequestForCityLevelEvent = HierarchicalGeoJsonDataRequiredEvent(
                    jobId = event.jobId,
                    countryIso3Code = oneCountryToImportRequest.countryIso3Code,
                    hierarchyDetailsRequest = oneCountryToImportRequest.importingDetailsForCity
                )
                logger.debug("Publishing Event to download data for $oneCountryToImportRequest.countryIso3Code, City Level, File Level No.: ${oneCountryToImportRequest.importingDetailsForCity.hierarchyLevelOfFileToImport}.")
                eventPublisher.publish(importRequestForCityLevelEvent)

            }

            if (oneCountryToImportRequest.importingDetailsForDistrict != null) {

                val importRequestForDistrictLevelEvent = HierarchicalGeoJsonDataRequiredEvent(
                    jobId = event.jobId,
                    countryIso3Code = oneCountryToImportRequest.countryIso3Code,
                    hierarchyDetailsRequest = oneCountryToImportRequest.importingDetailsForDistrict
                )
                logger.debug("Publishing Event to download data for $oneCountryToImportRequest.countryIso3Code, District Level, File Level No.: ${oneCountryToImportRequest.importingDetailsForDistrict.hierarchyLevelOfFileToImport}.")
                eventPublisher.publish(importRequestForDistrictLevelEvent)

            }

        }

        logger.info("All download tasks for job ${event.jobId} have been dispatched.")
    }

    @KafkaListener(
        topics = ["\${topics.geolocation.geojson-download.hierarchical-geo-json-data-required}"],
        groupId = "geolocation-downloader-worker"
    )
    fun handleCountryDataRequired(hierarchicalGeoJsonDataRequiredEvent: HierarchicalGeoJsonDataRequiredEvent) = runBlocking {

        val jobId = hierarchicalGeoJsonDataRequiredEvent.jobId
        val targetCountryCode = hierarchicalGeoJsonDataRequiredEvent.countryIso3Code
        val hierarchyDetailsRequest: HierarchyDetailsRequest = hierarchicalGeoJsonDataRequiredEvent.hierarchyDetailsRequest
        val targetLevel = hierarchicalGeoJsonDataRequiredEvent.hierarchyDetailsRequest.hierarchyLevelOfFileToImport

        logger.info("Received request to download GeoJSON for $targetCountryCode, level $targetLevel (Job ID: $jobId)")

        try {
            if (!geoJsonProvider.fileExists(targetCountryCode, targetLevel)) {
                throw FileNotFoundException("File not found for $targetCountryCode at level $targetLevel.")
            }

            val tempPath = geoJsonProvider.downloadFile(targetCountryCode, targetLevel)
            val fileName = geoJsonProvider.getFileName(targetCountryCode, targetLevel)

            val successEvent = GeoJsonFileDownloadedEvent(
                jobId = jobId,
                countryIso3Code = targetCountryCode,
                hierarchyDetailsRequest = hierarchyDetailsRequest,
                tempFilePath = tempPath.toString(),
                fileName = fileName
            )
            eventPublisher.publish(successEvent)
            logger.info("Successfully downloaded and published success event for $targetCountryCode, level $targetLevel.")

        } catch (e: Exception) {
            logger.error("Failed to download GeoJSON for $targetCountryCode, level $targetLevel (Job ID: $jobId). Reason: ${e.message}")
            val failureEvent = GeoJsonDownloadFailedEvent(
                jobId = jobId,
                countryIso3Code = targetCountryCode,
                hierarchyDetailsRequest = hierarchyDetailsRequest,
                reason = e.message ?: "Unknown error"
            )
            eventPublisher.publish(failureEvent)
        }
    }

    @KafkaListener(
        topics = ["\${topics.geolocation.geojson-file.downloaded}"],
        groupId = "geolocation-uploader-worker"
    )
    suspend fun handleFileDownloaded(geoJsonFileDownloadedEvent: GeoJsonFileDownloadedEvent) {
        val jobId = geoJsonFileDownloadedEvent.jobId
        val targetCountryCode = geoJsonFileDownloadedEvent.countryIso3Code
        val hierarchyDetailsRequest: HierarchyDetailsRequest = geoJsonFileDownloadedEvent.hierarchyDetailsRequest
        val targetLevel = geoJsonFileDownloadedEvent.hierarchyDetailsRequest.hierarchyLevelOfFileToImport
        val tempFilePathStr = geoJsonFileDownloadedEvent.tempFilePath
        val fileName = geoJsonFileDownloadedEvent.fileName

        logger.info("Received downloaded file event for $targetCountryCode, level $targetLevel. Uploading to asset manager... (Job ID: $jobId)")
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
                    "country" to targetCountryCode,
                    "level" to targetLevel.toString(),
                    "source" to "GADMv4.1"
                )
            )

            val uploadAssetResponseDto = assetUseCase.uploadGenericAsset(
                assetUploadRequestDto,
                targetCountryCode,
                hierarchyDetailsRequest
            )

            logger.info("Successfully uploaded $fileName to the StorageProvider via AssetManager :: Response: $uploadAssetResponseDto ")

        } catch (e: Exception) {
            logger.error("Failed to upload $fileName for job $jobId. Reason: ${e.message}")
            val failureEvent = GeoJsonDownloadFailedEvent(
                jobId = jobId,
                countryIso3Code = targetCountryCode,
                hierarchyDetailsRequest = hierarchyDetailsRequest,
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