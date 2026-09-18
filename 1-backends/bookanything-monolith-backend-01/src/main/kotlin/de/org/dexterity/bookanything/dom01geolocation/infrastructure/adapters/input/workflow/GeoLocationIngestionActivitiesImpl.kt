package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationAIEnrichmentService
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationAssetIntegrationService
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationSvgGeneratorService
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionActivities
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportSummaryDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationDetailReportResultDto
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.temporal.activity.Activity
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Component
class GeoLocationIngestionActivitiesImpl(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val geoLocationCRUDService: GeoLocationCRUDService,
    private val svgGeneratorService: GeoLocationSvgGeneratorService,
    private val assetIntegrationService: GeoLocationAssetIntegrationService,
    private val aiEnrichmentService: GeoLocationAIEnrichmentService,
    @Value("\${topics.geolocation.nifi-import.requested:geolocation.nifi-import.requested}")
    private val nifiImportRequestedTopic: String,
    @Value("\${topics.geolocation.nifi-import.completed:geolocation.nifi-import.completed}")
    private val nifiImportCompletedTopic: String,
    @Value("\${topics.geolocation.batch-import.completed:geolocation.batch-import.completed}")
    private val batchImportCompletedTopic: String,
    @Value("\${nifi.base-url:https://nifi.darueira-corpshared.127.0.0.1.nip.io}")
    private val nifiBaseUrl: String,
    @Value("\${nifi.import-timeout-minutes:25}")
    private val nifiImportTimeoutMinutes: Long,
    @Value("\${jsreport.base-url:http://10.152.183.35:5488}")
    private val jsreportBaseUrl: String,
    @Value("\${jsreport.username:admin}")
    private val jsreportUsername: String,
    @Value("\${jsreport.password:change-me-in-openbao}")
    private val jsreportPassword: String,
    @Value("\${jsreport.template-name:geolocation-batch-summary}")
    private val jsreportTemplateName: String,
    @Value("\${jsreport.detail-template-name:geolocation-detail-report}")
    private val jsreportDetailTemplateName: String = "geolocation-detail-report",
    @Value("\${corporate.minio.endpoint:http://central-minio.drr-corpshared-plat.svc.cluster.local:9000}")
    private val corporateMinioEndpoint: String,
    @Value("\${corporate.minio.access-key:minioadmin}")
    private val corporateMinioAccessKey: String,
    @Value("\${corporate.minio.secret-key:Darueira@2026!}")
    private val corporateMinioSecretKey: String,
    @Value("\${corporate.minio.reports-bucket:darueira-reports}")
    private val corporateMinioReportsBucket: String
) : GeoLocationIngestionActivities {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val pendingReplies = ConcurrentHashMap<String, CompletableFuture<CountryImportSummaryDto>>()

    @KafkaListener(
        topics = ["\${topics.geolocation.nifi-import.completed:geolocation.nifi-import.completed}"],
        groupId = "bookanything-nifi-reply-group",
        properties = [
            "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
        ]
    )
    fun onNiFiCompleted(record: String) {
        try {
            logger.info("Kafka Listener: Received NiFi completion record: $record")
            val summary = objectMapper.readValue(record, CountryImportSummaryDto::class.java)
            val correlationKey = "${summary.countrySlug}_${summary.locationLevel}"
            val future = pendingReplies[correlationKey]
            if (future != null) {
                future.complete(summary)
                logger.info("Kafka Listener: Successfully matched and resolved future for correlationKey=$correlationKey")
            } else {
                logger.warn("Kafka Listener: No pending future found for correlationKey=$correlationKey. It may have timed out or already completed.")
            }
        } catch (e: Exception) {
            logger.error("Kafka Listener: Error processing NiFi completion message: $record", e)
        }
    }

    override fun processCountryLocationViaNiFi(item: CountryImportItem): CountryImportSummaryDto {
        val correlationKey = "${item.countrySlug}_${item.locationLevel}"
        val future = CompletableFuture<CountryImportSummaryDto>()
        pendingReplies[correlationKey] = future

        val jobId = try {
            Activity.getExecutionContext()?.info?.workflowId ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }

        logger.info("Temporal Activity: Starting NiFi Ingestion for ${item.countrySlug} (Level ${item.locationLevel}) [XML=${item.shouldConvertToXML}], JobId=$jobId")

        val payload = mapOf(
            "jobId" to jobId,
            "countrySlug" to item.countrySlug,
            "locationLevel" to item.locationLevel,
            "shouldConvertToXML" to item.shouldConvertToXML
        )

        try {
            kafkaTemplate.send(nifiImportRequestedTopic, item.countrySlug, payload)
            kafkaTemplate.flush()
            logger.info("Temporal Activity: Published NiFi trigger to $nifiImportRequestedTopic for ${item.countrySlug} level ${item.locationLevel}")

            // Wait up to configured timeout (default 25 minutes) for NiFi to process and publish response
            val summary = future.get(nifiImportTimeoutMinutes, TimeUnit.MINUTES)
            logger.info("Temporal Activity: Ingestion finished for ${item.countrySlug} (Created=${summary.createdCount}, Updated=${summary.updatedCount}, Duration=${summary.durationMs}ms)")
            return summary
        } catch (e: Exception) {
            logger.error("Temporal Activity: Timeout or failure awaiting NiFi ingestion for ${item.countrySlug} (level ${item.locationLevel})", e)
            return CountryImportSummaryDto(
                countrySlug = item.countrySlug,
                locationLevel = item.locationLevel,
                rawGeoJsonUrl = "s3://darueira-geodata/raw/gadm41_${item.countrySlug}_${item.locationLevel}.json",
                convertedXmlUrl = if (item.shouldConvertToXML) "s3://darueira-geodata/xml/gadm41_${item.countrySlug}_${item.locationLevel}.xml" else null,
                createdCount = 0,
                updatedCount = 0,
                durationMs = 0,
                errorMessage = e.message ?: "Failed awaiting NiFi processing"
            )
        } finally {
            pendingReplies.remove(correlationKey)
        }
    }

    override fun generateJsReportSummaryPdf(jobId: String, summaries: List<CountryImportSummaryDto>): String {
        logger.info("Temporal Activity: Requesting JSReport PDF generation for Job $jobId at $jsreportBaseUrl (Items: ${summaries.size})")

        val totalCreated = summaries.sumOf { it.createdCount }
        val totalUpdated = summaries.sumOf { it.updatedCount }
        val totalDuration = summaries.sumOf { it.durationMs }

        val renderPayload = mapOf(
            "template" to mapOf("name" to jsreportTemplateName),
            "data" to mapOf(
                "jobId" to jobId,
                "generatedAt" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                "totalItems" to summaries.size,
                "totalCreated" to totalCreated,
                "totalUpdated" to totalUpdated,
                "totalDurationMs" to totalDuration,
                "summaries" to summaries
            )
        )

        val jsonBytes = objectMapper.writeValueAsBytes(renderPayload)
        val url = URI.create("$jsreportBaseUrl/api/report").toURL()
        val conn = url.openConnection() as HttpURLConnection
        val basicAuth = "Basic " + Base64.getEncoder().encodeToString("$jsreportUsername:$jsreportPassword".toByteArray())
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", basicAuth)
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 60000

        conn.outputStream.use { it.write(jsonBytes) }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            logger.error("JSReport rendering failed with HTTP $responseCode: $err")
            throw RuntimeException("Failed to generate JSReport PDF: $err")
        }

        val pdfBytes = conn.inputStream.use { it.readBytes() }
        logger.info("Temporal Activity: JSReport rendered PDF successfully (Size: ${pdfBytes.size} bytes)")

        val minioClient = MinioClient.builder()
            .endpoint(corporateMinioEndpoint)
            .credentials(corporateMinioAccessKey, corporateMinioSecretKey)
            .build()

        val bucketExists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(corporateMinioReportsBucket).build()
        )
        if (!bucketExists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(corporateMinioReportsBucket).build()
            )
        }

        val objectKey = "geolocations/job-${jobId}-summary.pdf"
        ByteArrayInputStream(pdfBytes).use { bais ->
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(corporateMinioReportsBucket)
                    .`object`(objectKey)
                    .stream(bais, pdfBytes.size.toLong(), -1)
                    .contentType("application/pdf")
                    .build()
            )
        }

        val finalUrl = "s3://$corporateMinioReportsBucket/$objectKey"
        logger.info("Temporal Activity: Generated and stored report artifact at $finalUrl")
        return finalUrl
    }

    override fun publishBatchImportCompletedEvent(result: GeoLocationBatchImportResult) {
        logger.info("Temporal Activity: Publishing completion event to Kafka for Job ${result.jobId} (Total Created: ${result.totalCreated}, Total Updated: ${result.totalUpdated})")
        kafkaTemplate.send(batchImportCompletedTopic, result.jobId, result)
        kafkaTemplate.flush()
        logger.info("Temporal Activity: Batch import completed event published to $batchImportCompletedTopic")
    }

    override fun generateGeoLocationArtifactsAndReport(geoLocationId: Long): GeoLocationDetailReportResultDto {
        logger.info("Activity: Starting artifact and report generation for GeoLocation #$geoLocationId...")

        // 1. Retrieve GeoLocation across hierarchy levels
        val geoLocation = geoLocationCRUDService.findAnyById(geoLocationId)
        if (geoLocation == null) {
            logger.error("Activity: GeoLocation #$geoLocationId not found across any hierarchy level.")
            return GeoLocationDetailReportResultDto(
                geoLocationId = geoLocationId,
                status = "ERROR",
                errorMessage = "GeoLocation not found for ID: $geoLocationId"
            )
        }

        val name = geoLocation.name
        val type = geoLocation.type.name
        val friendlyId = geoLocation.friendlyId
        val alias = geoLocation.alias

        // 2. Check boundary geometry
        val boundary = geoLocation.boundaryRepresentation
        if (boundary == null || boundary.isEmpty) {
            logger.warn("Activity: GeoLocation #$geoLocationId ($name) has no boundary geometry. Skipping artifacts and report.")
            return GeoLocationDetailReportResultDto(
                geoLocationId = geoLocationId,
                name = name,
                type = type,
                friendlyId = friendlyId,
                status = "SKIPPED_NO_GEOMETRY",
                errorMessage = "No geographic boundary geometry found (ge_geographic_boundary is null or empty)."
            )
        }

        try {
            // 3. Generate SVGs
            logger.info("Activity: Generating Local Vector Map and World Highlight SVG for #$geoLocationId ($name)...")
            val localMapSvg = svgGeneratorService.generateLocalMapSvg(boundary, name, alias ?: friendlyId)
            val worldHighlightSvg = svgGeneratorService.generateWorldHighlightMapSvg(boundary, name, alias ?: friendlyId)

            // 4. Save SVGs into Tenant MinIO using Asset Management Domain
            logger.info("Activity: Uploading SVG maps to Tenant MinIO for #$geoLocationId...")
            val (localAsset, worldAsset) = runBlocking {
                assetIntegrationService.saveGeoLocationMapSvgs(geoLocation, localMapSvg, worldHighlightSvg)
            }
            val localMapUrl = "s3://${localAsset.bucket.name}/${localAsset.storageKey}"
            val worldMapUrl = "s3://${worldAsset.bucket.name}/${worldAsset.storageKey}"
            logger.info("Activity: Both SVGs successfully persisted in Tenant MinIO (Local: Asset #${localAsset.id}, World: Asset #${worldAsset.id})")

            // 5. Spring AI Enrichment
            val parentModel = geoLocationCRUDService.findParentModel(geoLocation)
            val parentName = parentModel?.name
            logger.info("Activity: Requesting Spring AI synthesis for #$geoLocationId ($name) [Parent: $parentName]...")
            val aiSummary = aiEnrichmentService.enrichGeoLocation(geoLocation, parentName)

            try {
                val updatedDetails = (geoLocation.additionalDetailsMap ?: emptyMap()).toMutableMap()
                updatedDetails["ai_enrichment_summary"] = aiSummary
                geoLocationCRUDService.updateAdditionalDetails(geoLocation.type, geoLocationId, updatedDetails)
                logger.info("Activity: Persisted AI summary in additional details for GeoLocation #$geoLocationId")
            } catch (e: Exception) {
                logger.warn("Activity: Could not persist AI summary in entity details: ${e.message}")
            }

            // 6. Generate PDF via JSReport
            logger.info("Activity: Calling JSReport '$jsreportDetailTemplateName' for #$geoLocationId ($name)...")
            val env = boundary.envelopeInternal
            val envelopeStr = String.format(Locale.US, "[%.3f, %.3f] to [%.3f, %.3f]", env.minX, env.minY, env.maxX, env.maxY)

            val renderData = mapOf(
                "geoLocation" to mapOf(
                    "id" to geoLocation.id.id,
                    "name" to name,
                    "type" to type,
                    "alias" to (alias ?: "N/A"),
                    "friendlyId" to friendlyId,
                    "parentName" to (parentName ?: ""),
                    "parentId" to (geoLocation.parentId ?: ""),
                    "geometryType" to boundary.geometryType,
                    "pointsCount" to boundary.numPoints,
                    "envelope" to envelopeStr
                ),
                "localMapSvg" to localMapSvg,
                "localMapAssetId" to localAsset.id,
                "localMapAssetUrl" to localMapUrl,
                "worldHighlightSvg" to worldHighlightSvg,
                "worldHighlightAssetId" to worldAsset.id,
                "worldHighlightAssetUrl" to worldMapUrl,
                "aiEnrichment" to aiSummary,
                "generatedAt" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )

            val renderPayload = mapOf(
                "template" to mapOf("name" to jsreportDetailTemplateName),
                "data" to renderData
            )

            val jsonBytes = objectMapper.writeValueAsBytes(renderPayload)
            val url = URI.create("$jsreportBaseUrl/api/report").toURL()
            var pdfBytes: ByteArray? = null
            var lastErr: String? = null

            for (attempt in 1..3) {
                try {
                    val conn = url.openConnection() as HttpURLConnection
                    val basicAuth = "Basic " + Base64.getEncoder().encodeToString("$jsreportUsername:$jsreportPassword".toByteArray())
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", basicAuth)
                    conn.doOutput = true
                    conn.connectTimeout = 30000
                    conn.readTimeout = 60000

                    conn.outputStream.use { it.write(jsonBytes) }

                    val responseCode = conn.responseCode
                    if (responseCode in 200..299) {
                        pdfBytes = conn.inputStream.use { it.readBytes() }
                        break
                    } else {
                        lastErr = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                        logger.warn("JSReport render attempt $attempt failed with HTTP $responseCode: $lastErr")
                        Thread.sleep(1500)
                    }
                } catch (e: Exception) {
                    lastErr = e.message
                    logger.warn("JSReport render attempt $attempt encountered exception: $lastErr")
                    Thread.sleep(1500)
                }
            }

            if (pdfBytes == null) {
                logger.error("JSReport detail rendering failed after retries: $lastErr")
                throw RuntimeException("JSReport detail rendering failed: $lastErr")
            }

            logger.info("Activity: JSReport rendered detail PDF successfully (Size: ${pdfBytes.size} bytes)")

            // 7. Save PDF in Tenant MinIO
            val reportAsset = runBlocking {
                assetIntegrationService.saveGeoLocationReportPdf(geoLocation, pdfBytes)
            }
            val tenantReportUrl = "s3://${reportAsset.bucket.name}/${reportAsset.storageKey}"

            // 8. Redundantly store copy in Corporate MinIO reports bucket
            try {
                val minioClient = MinioClient.builder()
                    .endpoint(corporateMinioEndpoint)
                    .credentials(corporateMinioAccessKey, corporateMinioSecretKey)
                    .build()

                val bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(corporateMinioReportsBucket).build()
                )
                if (!bucketExists) {
                    minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(corporateMinioReportsBucket).build()
                    )
                }
                val corpObjectKey = "geolocations/detail-report-$geoLocationId.pdf"
                ByteArrayInputStream(pdfBytes).use { bais ->
                    minioClient.putObject(
                        PutObjectArgs.builder()
                            .bucket(corporateMinioReportsBucket)
                            .`object`(corpObjectKey)
                            .stream(bais, pdfBytes.size.toLong(), -1)
                            .contentType("application/pdf")
                            .build()
                    )
                }
                logger.info("Activity: Also stored detail report copy in Corporate MinIO at s3://$corporateMinioReportsBucket/$corpObjectKey")
            } catch (e: Exception) {
                logger.warn("Activity: Could not store redundant copy in Corporate MinIO: ${e.message}")
            }

            logger.info("Activity: Successfully completed artifacts and report for GeoLocation #$geoLocationId ($name)!")
            return GeoLocationDetailReportResultDto(
                geoLocationId = geoLocationId,
                name = name,
                type = type,
                friendlyId = friendlyId,
                localMapAssetId = localAsset.id,
                localMapAssetUrl = localMapUrl,
                worldHighlightAssetId = worldAsset.id,
                worldHighlightAssetUrl = worldMapUrl,
                reportPdfUrl = tenantReportUrl,
                aiEnrichmentSummary = aiSummary,
                status = "SUCCESS"
            )
        } catch (e: Exception) {
            logger.error("Activity: Failed generating artifacts and report for GeoLocation #$geoLocationId", e)
            return GeoLocationDetailReportResultDto(
                geoLocationId = geoLocationId,
                name = name,
                type = type,
                friendlyId = friendlyId,
                status = "ERROR",
                errorMessage = e.message ?: "Unknown error generating artifacts and report"
            )
        }
    }
}
