package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.application.services.MapSvgGeneratorMechanismHolder
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationArtifactsLauncher
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionActivities
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ArtifactsBatchSummary
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportSummaryDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.temporal.activity.Activity
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import io.temporal.client.ActivityCompletionException
import io.temporal.client.WorkflowFailedException
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
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
import java.util.concurrent.TimeoutException
import java.time.Duration

@Component
class GeoLocationIngestionActivitiesImpl(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val geoLocationCRUDService: GeoLocationCRUDService,
    private val artifactsLauncher: GeoLocationArtifactsLauncher,
    private val mechanismHolder: MapSvgGeneratorMechanismHolder,
    transactionManager: PlatformTransactionManager,
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
    @Value("\${corporate.minio.endpoint:http://central-minio.drr-corpshared-plat.svc.cluster.local:9000}")
    private val corporateMinioEndpoint: String,
    @Value("\${corporate.minio.access-key:minioadmin}")
    private val corporateMinioAccessKey: String,
    @Value("\${corporate.minio.secret-key:Darueira@2026!}")
    private val corporateMinioSecretKey: String,
    @Value("\${corporate.minio.reports-bucket:darueira-reports}")
    private val corporateMinioReportsBucket: String,
    @Value("\${temporal.artifacts.await-timeout-minutes:30}")
    private val artifactsAwaitTimeoutMinutes: Long = 30
) : GeoLocationIngestionActivities {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val pendingReplies = ConcurrentHashMap<String, CompletableFuture<CountryImportSummaryDto>>()

    // Worker threads have no Hibernate session and the JPA mappers read lazy collections: read in a short transaction.
    private val readTx = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    override fun configureMapSvgGeneratorMechanism(mechanism: String) {
        logger.info("Temporal Activity: Configured map SVG generator mechanism to: $mechanism")
        mechanismHolder.current = mechanism
    }

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

            // Wait up to configured timeout (default 25 minutes) for NiFi to process and publish
            // response, heartbeating so Temporal can detect a lost task or dead worker early.
            val summary = awaitWithHeartbeat(future, nifiImportTimeoutMinutes)
            logger.info("Temporal Activity: Ingestion finished for ${item.countrySlug} (Created=${summary.createdCount}, Updated=${summary.updatedCount}, Duration=${summary.durationMs}ms)")
            return summary
        } catch (e: ActivityCompletionException) {
            // Cancelled or timed out on the Temporal side: let the SDK handle it.
            throw e
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

    private fun <T> awaitWithHeartbeat(future: CompletableFuture<T>, timeoutMinutes: Long): T {
        val context = try { Activity.getExecutionContext() } catch (e: IllegalStateException) { null }
            ?: return future.get(timeoutMinutes, TimeUnit.MINUTES)
        val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(timeoutMinutes)
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) throw TimeoutException("No NiFi reply within $timeoutMinutes minutes")
            try {
                return future.get(minOf(remaining, TimeUnit.SECONDS.toNanos(15)), TimeUnit.NANOSECONDS)
            } catch (e: TimeoutException) {
                context.heartbeat<Any?>(null)
            }
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

    override fun awaitArtifactWorkflows(item: CountryImportItem): ArtifactsBatchSummary {
        val ids = geoLocationIdsOf(item)
        logger.info("Temporal Activity: Waiting for the artifacts of ${ids.size} GeoLocation(s) of ${item.countrySlug} level ${item.locationLevel}...")

        // Make sure every item has a workflow. Running or just-finished ones are kept (no duplicates).
        ids.forEach { artifactsLauncher.start(it) }

        var succeeded = 0
        var skipped = 0
        val failedIds = mutableListOf<Long>()
        val context = Activity.getExecutionContext()
        ids.forEachIndexed { index, id ->
            context.heartbeat("${index}/${ids.size}")
            when (awaitArtifacts(id)) {
                "SUCCESS" -> succeeded++
                "SKIPPED_NO_GEOMETRY" -> skipped++
                else -> failedIds += id
            }
        }
        logger.info("Temporal Activity: Artifacts of ${item.countrySlug} level ${item.locationLevel}: total=${ids.size}, succeeded=$succeeded, skipped=$skipped, failed=${failedIds.size}")
        return ArtifactsBatchSummary(
            countrySlug = item.countrySlug,
            locationLevel = item.locationLevel,
            total = ids.size,
            succeeded = succeeded,
            skipped = skipped,
            failed = failedIds.size,
            failedIds = failedIds
        )
    }

    /** Waits for one item in slices of 30 s (heartbeating) up to [artifactsAwaitTimeoutMinutes]. */
    private fun awaitArtifacts(geoLocationId: Long): String {
        val context = Activity.getExecutionContext()
        val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(artifactsAwaitTimeoutMinutes)
        while (true) {
            try {
                return artifactsLauncher.awaitResult(geoLocationId, Duration.ofSeconds(30)).status
            } catch (e: TimeoutException) {
                context.heartbeat("waiting #$geoLocationId")
                if (System.nanoTime() > deadline) {
                    logger.warn("Temporal Activity: Gave up waiting for the artifacts of GeoLocation #$geoLocationId")
                    return "TIMED_OUT"
                }
            } catch (e: WorkflowFailedException) {
                logger.warn("Temporal Activity: Artifacts workflow of GeoLocation #$geoLocationId failed: ${e.cause?.message ?: e.message}")
                return "FAILED"
            }
        }
    }

    /**
     * GeoLocation ids of the country/level, read from the database (the NiFi counters proved unreliable:
     * the USA level 1 summary said 36 created while all 51 states were persisted).
     */
    private fun geoLocationIdsOf(item: CountryImportItem): List<Long> {
        val slug = item.countrySlug
        val country = readTx.execute {
            geoLocationCRUDService
                .findByFriendlyId(GeoLocationType.COUNTRY, slug, PageRequest.of(0, 20))
                .content.firstOrNull { it.friendlyId.equals(slug, ignoreCase = true) }
        }
        if (country == null) {
            logger.warn("Temporal Activity: Country '$slug' not found; no artifacts to wait for")
            return emptyList()
        }
        val countryId = country.id.id
        return when (item.locationLevel) {
            0 -> listOf(countryId)
            1 -> {
                val ids = mutableListOf<Long>()
                var page = 0
                do {
                    val result = readTx.execute {
                        geoLocationCRUDService.searchByParentIdAndNameStartingWith(
                            GeoLocationType.PROVINCE, countryId, "", PageRequest.of(page, 200)
                        ).let { Pair(it.content.map { model -> model.id.id }, it.hasNext()) }
                    }!!
                    ids += result.first
                    page++
                } while (result.second)
                ids
            }
            else -> {
                logger.warn("Temporal Activity: Location level ${item.locationLevel} of '$slug' is not supported for artifacts; skipping")
                emptyList()
            }
        }
    }
}
