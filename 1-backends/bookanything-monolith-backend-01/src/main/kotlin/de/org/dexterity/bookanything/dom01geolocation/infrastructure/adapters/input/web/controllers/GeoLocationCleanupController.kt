package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationCleanupWorkflow
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationCleanupRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationCleanupResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowFailedException
import io.temporal.client.WorkflowOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Starts and follows the cleanup of imported GeoLocations (ADR-0015). Dry run by default: the request only
 * deletes something when it sends `"dryRun": false`. No OIDC/OpenFGA yet (backlog item 24).
 */
@RestController
@RequestMapping("/api/v1/geolocations/workflows/cleanup")
class GeoLocationCleanupController(
    private val workflowClient: WorkflowClient,
    @Value("\${temporal.task-queue:GEOLOCATION_INGESTION_TASK_QUEUE}")
    private val taskQueue: String
) {

    @PostMapping
    fun startCleanup(@RequestBody request: GeoLocationCleanupRequest): ResponseEntity<Map<String, Any>> {
        if (request.items.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "The request has no items"))
        }
        val workflowId = "geo-cleanup-${UUID.randomUUID()}"
        val stub = workflowClient.newWorkflowStub(
            GeoLocationCleanupWorkflow::class.java,
            WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(taskQueue).build()
        )
        WorkflowClient.start(stub::executeCleanup, request)
        return ResponseEntity.accepted().body(
            mapOf(
                "workflowId" to workflowId,
                "taskQueue" to taskQueue,
                "dryRun" to request.dryRunEnabled(),
                "statusUrl" to "/api/v1/geolocations/workflows/cleanup/$workflowId/status",
                "resultUrl" to "/api/v1/geolocations/workflows/cleanup/$workflowId/result",
                "message" to if (request.dryRunEnabled()) "Dry run accepted: the plan is computed and nothing is deleted."
                else "Cleanup accepted and running asynchronously in Temporal."
            )
        )
    }

    @GetMapping("/{workflowId}/status")
    fun status(@PathVariable workflowId: String): ResponseEntity<ImportProgressDto> {
        val stub = workflowClient.newWorkflowStub(GeoLocationCleanupWorkflow::class.java, workflowId)
        return ResponseEntity.ok(stub.getProgress())
    }

    @GetMapping("/{workflowId}/result")
    fun result(@PathVariable workflowId: String): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(
                workflowClient.newUntypedWorkflowStub(workflowId)
                    .getResult(60, TimeUnit.SECONDS, GeoLocationCleanupResult::class.java)
            )
        } catch (e: TimeoutException) {
            ResponseEntity.accepted().body(mapOf("workflowId" to workflowId, "message" to "Still running: ask again later"))
        } catch (e: WorkflowFailedException) {
            ResponseEntity.status(500).body(mapOf("workflowId" to workflowId, "message" to (e.cause?.message ?: e.message ?: "Cleanup workflow failed")))
        }
}
