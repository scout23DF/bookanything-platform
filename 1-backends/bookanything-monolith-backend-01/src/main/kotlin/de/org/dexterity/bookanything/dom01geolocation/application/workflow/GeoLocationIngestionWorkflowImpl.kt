package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportSummaryDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class GeoLocationIngestionWorkflowImpl : GeoLocationIngestionWorkflow {

    private val retryOptions = RetryOptions.newBuilder()
        .setInitialInterval(Duration.ofSeconds(2))
        .setBackoffCoefficient(2.0)
        .setMaximumInterval(Duration.ofMinutes(1))
        .setMaximumAttempts(3)
        .build()

    private val activityOptions = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(30))
        .setRetryOptions(retryOptions)
        .build()

    private val activities = Workflow.newActivityStub(
        GeoLocationIngestionActivities::class.java,
        activityOptions
    )

    private var progress = ImportProgressDto(
        currentStage = "PENDING",
        percentCompleted = 0,
        processedItems = 0,
        totalItems = 0,
        status = "NOT_STARTED",
        details = null
    )

    override fun executeBatchImport(request: GeoLocationBatchImportRequest): GeoLocationBatchImportResult {
        val workflowId = Workflow.getInfo().workflowId
        val totalItems = request.items.size

        progress = ImportProgressDto(
            currentStage = "STARTED",
            percentCompleted = 5,
            processedItems = 0,
            totalItems = totalItems,
            status = "RUNNING",
            details = "Starting batch ingestion for $totalItems items"
        )

        val summaries = mutableListOf<CountryImportSummaryDto>()

        request.items.forEachIndexed { index, item ->
            val countryCode = item.countrySlug
            val level = item.locationLevel

            progress = ImportProgressDto(
                currentStage = "NIFI_INGESTION",
                percentCompleted = 10 + ((index.toDouble() / totalItems.toDouble()) * 70).toInt(),
                processedItems = index,
                totalItems = totalItems,
                status = "RUNNING",
                details = "Processing $countryCode (level $level) via NiFi [${index + 1}/$totalItems]"
            )

            val summary = activities.processCountryLocationViaNiFi(item)
            summaries.add(summary)
        }

        progress = ImportProgressDto(
            currentStage = "JSREPORT_GENERATION",
            percentCompleted = 85,
            processedItems = totalItems,
            totalItems = totalItems,
            status = "RUNNING",
            details = "Generating executive PDF summary report via JSReport"
        )

        val reportPdfUrl = activities.generateJsReportSummaryPdf(workflowId, summaries)

        val totalCreated = summaries.sumOf { it.createdCount }
        val totalUpdated = summaries.sumOf { it.updatedCount }

        val finalResult = GeoLocationBatchImportResult(
            jobId = workflowId,
            totalCreated = totalCreated,
            totalUpdated = totalUpdated,
            reportPdfUrl = reportPdfUrl,
            countrySummaries = summaries,
            status = "COMPLETED"
        )

        progress = ImportProgressDto(
            currentStage = "PUBLISHING_EVENT",
            percentCompleted = 95,
            processedItems = totalItems,
            totalItems = totalItems,
            status = "RUNNING",
            details = "Publishing batch import completion event to Kafka"
        )

        activities.publishBatchImportCompletedEvent(finalResult)

        progress = ImportProgressDto(
            currentStage = "COMPLETED",
            percentCompleted = 100,
            processedItems = totalItems,
            totalItems = totalItems,
            status = "COMPLETED",
            details = "Workflow executed successfully. Report: $reportPdfUrl"
        )

        return finalResult
    }

    override fun getProgress(): ImportProgressDto {
        return progress
    }
}
