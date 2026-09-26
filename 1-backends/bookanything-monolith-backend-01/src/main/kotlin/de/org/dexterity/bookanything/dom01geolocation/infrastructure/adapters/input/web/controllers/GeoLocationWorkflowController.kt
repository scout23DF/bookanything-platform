package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationArtifactsLauncher
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionActivities
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionWorkflow
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationDetailReportResultDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowFailedException
import io.temporal.client.WorkflowOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeoutException

@RestController
@RequestMapping("/api/v1/geolocations/workflows")
class GeoLocationWorkflowController(
    private val workflowClient: WorkflowClient,
    private val activities: GeoLocationIngestionActivities,
    private val artifactsLauncher: GeoLocationArtifactsLauncher,
    @Value("\${temporal.task-queue:GEOLOCATION_INGESTION_TASK_QUEUE}")
    private val taskQueue: String
) {

    @PostMapping(value = ["", "/batch-import"])
    fun triggerBatchImportWorkflow(
        @RequestBody request: GeoLocationBatchImportRequest
    ): ResponseEntity<Map<String, Any>> {
        val workflowId = "geo-import-${UUID.randomUUID()}"

        val options = WorkflowOptions.newBuilder()
            .setWorkflowId(workflowId)
            .setTaskQueue(taskQueue)
            .build()

        val workflowStub = workflowClient.newWorkflowStub(GeoLocationIngestionWorkflow::class.java, options)

        // Asynchronous start of workflow
        WorkflowClient.start(workflowStub::executeBatchImport, request)

        return ResponseEntity.accepted().body(
            mapOf(
                "workflowId" to workflowId,
                "taskQueue" to taskQueue,
                "statusUrl" to "/api/v1/geolocations/workflows/batch-import/$workflowId/status",
                "message" to "GeoLocation batch import workflow accepted and running asynchronously in Temporal."
            )
        )
    }

    @GetMapping(value = ["/{workflowId}/status", "/batch-import/{workflowId}/status"])
    fun getWorkflowStatus(@PathVariable workflowId: String): ResponseEntity<ImportProgressDto> {
        val workflowStub = workflowClient.newWorkflowStub(GeoLocationIngestionWorkflow::class.java, workflowId)
        val progress = workflowStub.getProgress()
        return ResponseEntity.ok(progress)
    }

    @GetMapping(value = ["/{workflowId}/result", "/batch-import/{workflowId}/result"])
    fun getWorkflowResult(@PathVariable workflowId: String): ResponseEntity<GeoLocationBatchImportResult> {
        val untypedStub = workflowClient.newUntypedWorkflowStub(workflowId)
        val result = untypedStub.getResult(GeoLocationBatchImportResult::class.java)
        return ResponseEntity.ok(result)
    }

    @PostMapping(value = ["/artifacts-and-report/{geoLocationId}", "/{geoLocationId}/artifacts-and-report", "/batch-import/artifacts-and-report/{geoLocationId}"])
    fun generateGeoLocationArtifactsAndReport(
        @PathVariable geoLocationId: Long,
        @RequestParam(required = false) mapSvgGeneratorMechanism: String?
    ): ResponseEntity<GeoLocationDetailReportResultDto> {
        if (!mapSvgGeneratorMechanism.isNullOrBlank()) {
            activities.configureMapSvgGeneratorMechanism(mapSvgGeneratorMechanism)
        }
        // Same workflow the Kafka consumer and the import use (visible in the Temporal UI as
        // geo-artifacts-<id>); an explicit request always starts a new run, joining one that is in progress.
        val result = try {
            artifactsLauncher.start(geoLocationId, force = true)
            artifactsLauncher.awaitResult(geoLocationId, Duration.ofMinutes(5))
        } catch (e: WorkflowFailedException) {
            GeoLocationDetailReportResultDto(
                geoLocationId = geoLocationId, status = "ERROR",
                errorMessage = e.cause?.message ?: e.message ?: "Artifacts workflow failed"
            )
        } catch (e: TimeoutException) {
            GeoLocationDetailReportResultDto(
                geoLocationId = geoLocationId, status = "ERROR",
                errorMessage = "Timed out waiting for the artifacts workflow geo-artifacts-$geoLocationId (it keeps running)"
            )
        }
        return when (result.status) {
            "SUCCESS" -> ResponseEntity.ok(result)
            "SKIPPED_NO_GEOMETRY" -> ResponseEntity.status(422).body(result)
            else -> ResponseEntity.status(500).body(result)
        }
    }
}
