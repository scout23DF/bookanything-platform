package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCleanupService
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationCleanupActivities
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupDeletionSummary
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemPlan
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.NiFiCleanupSummaryDto
import io.temporal.activity.Activity
import io.temporal.client.ActivityCompletionException
import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Activities of the cleanup workflow (ADR-0015). The data logic is in [GeoLocationCleanupService]. */
@Component
class GeoLocationCleanupActivitiesImpl(
    private val cleanupService: GeoLocationCleanupService,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    @Value("\${topics.geolocation.nifi-cleanup.requested:geolocation.nifi-cleanup.requested}")
    private val nifiCleanupRequestedTopic: String,
    @Value("\${nifi.cleanup-timeout-minutes:5}")
    private val nifiCleanupTimeoutMinutes: Long
) : GeoLocationCleanupActivities {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val pendingReplies = ConcurrentHashMap<String, CompletableFuture<NiFiCleanupSummaryDto>>()

    private fun correlationKey(jobId: String, slug: String, level: Int) = "$jobId|${slug}_$level"

    override fun planCleanup(item: CleanupItem): CleanupItemPlan = cleanupService.plan(item)

    override fun deleteAssetsAndGeoLocations(item: CleanupItem): CleanupDeletionSummary =
        try {
            cleanupService.deleteData(item)
        } catch (e: DataIntegrityViolationException) {
            throw ApplicationFailure.newNonRetryableFailure(
                "A GeoLocation of ${item.countrySlug} level ${item.locationLevel} is still referenced by other data " +
                    "(addresses or cities): ${e.mostSpecificCause.message}",
                "GEOLOCATION_IN_USE"
            )
        }

    override fun verifyCleanup(item: CleanupItem): CleanupItemResult {
        val (geoLocations, assets) = cleanupService.remaining(item)
        return CleanupItemResult(
            countrySlug = item.countrySlug.uppercase(), locationLevel = item.locationLevel,
            remainingGeoLocations = geoLocations, remainingAssets = assets
        )
    }

    @KafkaListener(
        topics = ["\${topics.geolocation.nifi-cleanup.completed:geolocation.nifi-cleanup.completed}"],
        groupId = "bookanything-nifi-cleanup-reply-group",
        properties = ["value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"]
    )
    fun onNiFiCleanupCompleted(record: String) {
        try {
            logger.info("Kafka Listener: Received NiFi cleanup record: $record")
            val summary = objectMapper.readValue(record, NiFiCleanupSummaryDto::class.java)
            val future = pendingReplies[correlationKey(summary.jobId, summary.countrySlug, summary.locationLevel)]
            if (future != null) future.complete(summary)
            else logger.warn("Kafka Listener: No pending cleanup for job=${summary.jobId} ${summary.countrySlug} level ${summary.locationLevel}")
        } catch (e: Exception) {
            logger.error("Kafka Listener: Error processing NiFi cleanup message: $record", e)
        }
    }

    override fun deleteGeoDataFilesViaNiFi(item: CleanupItem): NiFiCleanupSummaryDto {
        val slug = item.countrySlug.uppercase()
        val jobId = try {
            Activity.getExecutionContext()?.info?.workflowId ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
        val key = correlationKey(jobId, slug, item.locationLevel)
        val future = CompletableFuture<NiFiCleanupSummaryDto>()
        pendingReplies[key] = future
        try {
            val payload = mapOf(
                "jobId" to jobId,
                "countrySlug" to slug,
                "locationLevel" to item.locationLevel,
                "shouldDeleteJsonFile" to item.shouldDeleteJsonFile,
                "shouldDeleteXmlFile" to item.shouldDeleteXmlFile
            )
            kafkaTemplate.send(nifiCleanupRequestedTopic, "${slug}_${item.locationLevel}", payload)
            kafkaTemplate.flush()
            logger.info("Temporal Activity: Published NiFi cleanup request to $nifiCleanupRequestedTopic for $slug level ${item.locationLevel} (json=${item.shouldDeleteJsonFile}, xml=${item.shouldDeleteXmlFile})")
            return await(future)
        } finally {
            pendingReplies.remove(key)
        }
    }

    /** Waits for the NiFi reply heartbeating; no reply in time is an error, so Temporal retries the (idempotent) request. */
    private fun await(future: CompletableFuture<NiFiCleanupSummaryDto>): NiFiCleanupSummaryDto {
        val context = Activity.getExecutionContext()
        val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(nifiCleanupTimeoutMinutes)
        while (true) {
            try {
                return future.get(20, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                try {
                    context.heartbeat("waiting for NiFi")
                } catch (c: ActivityCompletionException) {
                    throw c
                }
                if (System.nanoTime() > deadline) {
                    throw ApplicationFailure.newFailure(
                        "No reply from the NiFi cleanup flow within $nifiCleanupTimeoutMinutes minute(s)", "NIFI_NO_REPLY"
                    )
                }
            }
        }
    }
}
