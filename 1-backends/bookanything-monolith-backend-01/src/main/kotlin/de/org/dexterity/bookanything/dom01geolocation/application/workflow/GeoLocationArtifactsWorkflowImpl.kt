package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationDetailReportResultDto
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.failure.ActivityFailure
import io.temporal.workflow.Workflow
import java.time.Duration

class GeoLocationArtifactsWorkflowImpl : GeoLocationArtifactsWorkflow {

    private val logger = Workflow.getLogger(GeoLocationArtifactsWorkflowImpl::class.java)

    private fun stub(startToClose: Duration, maxAttempts: Int, initialInterval: Duration): GeoLocationArtifactsActivities =
        Workflow.newActivityStub(
            GeoLocationArtifactsActivities::class.java,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(startToClose)
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(initialInterval)
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofSeconds(60))
                        .setMaximumAttempts(maxAttempts)
                        // A missing GeoLocation will not appear by retrying.
                        .setDoNotRetry("GEOLOCATION_NOT_FOUND")
                        .build()
                )
                .build()
        )

    // GeoPandas subprocess + MinIO uploads.
    private val mapsActivities = stub(Duration.ofMinutes(5), maxAttempts = 3, initialInterval = Duration.ofSeconds(5))
    // Flag providers (Wikidata/Commons/flagcdn) + upload.
    private val flagActivities = stub(Duration.ofMinutes(2), maxAttempts = 3, initialInterval = Duration.ofSeconds(3))
    // Remote AI / Wikipedia summary.
    private val aiActivities = stub(Duration.ofMinutes(3), maxAttempts = 3, initialInterval = Duration.ofSeconds(5))
    // JSReport (Chrome) fails now and then with "Target closed": a few more attempts, only this step repeats.
    private val reportActivities = stub(Duration.ofMinutes(3), maxAttempts = 5, initialInterval = Duration.ofSeconds(2))
    private val copyActivities = stub(Duration.ofMinutes(2), maxAttempts = 3, initialInterval = Duration.ofSeconds(3))

    private var currentStep = "PENDING"

    override fun generate(geoLocationId: Long): GeoLocationDetailReportResultDto {
        currentStep = "MAPS"
        val maps = mapsActivities.generateAndStoreMaps(geoLocationId)
        if (maps.status == "SKIPPED_NO_GEOMETRY") {
            currentStep = "DONE"
            return GeoLocationDetailReportResultDto(
                geoLocationId = geoLocationId,
                name = maps.name,
                type = maps.type,
                friendlyId = maps.friendlyId,
                status = "SKIPPED_NO_GEOMETRY",
                errorMessage = maps.errorMessage
            )
        }

        currentStep = "FLAG"
        val flag = flagActivities.resolveAndStoreFlag(geoLocationId)

        currentStep = "AI_SUMMARY"
        val aiSummary = aiActivities.enrichWithAi(geoLocationId)

        currentStep = "REPORT"
        val report = reportActivities.renderAndStoreReport(geoLocationId, maps, flag, aiSummary)

        // The corporate copy is redundant: its failure must not fail the item.
        currentStep = "CORPORATE_COPY"
        try {
            copyActivities.copyReportToCorporateStorage(geoLocationId, report.reportAssetId)
        } catch (e: ActivityFailure) {
            logger.warn("Corporate copy of the report of GeoLocation #{} failed: {}", geoLocationId, e.cause?.message ?: e.message)
        }

        currentStep = "DONE"
        return GeoLocationDetailReportResultDto(
            geoLocationId = geoLocationId,
            name = maps.name,
            type = maps.type,
            friendlyId = maps.friendlyId,
            localMapAssetId = maps.localMapAssetId,
            localMapAssetUrl = maps.localMapAssetUrl,
            worldHighlightAssetId = maps.worldHighlightAssetId,
            worldHighlightAssetUrl = maps.worldHighlightAssetUrl,
            flagAssetId = flag.flagAssetId,
            flagAssetUrl = flag.flagAssetUrl,
            flagName = flag.flagName,
            reportPdfUrl = report.reportUrl,
            aiEnrichmentSummary = aiSummary,
            status = "SUCCESS"
        )
    }

    override fun getCurrentStep(): String = currentStep
}
