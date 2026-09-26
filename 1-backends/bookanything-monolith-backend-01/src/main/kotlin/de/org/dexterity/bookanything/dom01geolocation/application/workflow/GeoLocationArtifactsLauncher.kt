package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationDetailReportResultDto
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.temporal.api.enums.v1.WorkflowExecutionStatus
import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowExecutionAlreadyStarted
import io.temporal.client.WorkflowNotFoundException
import io.temporal.client.WorkflowOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Starts and waits for the per-GeoLocation [GeoLocationArtifactsWorkflow] (ADR-0014).
 *
 * The workflow id is deterministic (`geo-artifacts-<id>`), so several callers (the Kafka enrichment
 * consumer, the import workflow, the REST endpoint) can ask for the same GeoLocation without duplicating work.
 */
@Component
class GeoLocationArtifactsLauncher(
    private val workflowClient: WorkflowClient,
    @Value("\${temporal.artifacts.task-queue:GEOLOCATION_ARTIFACTS_TASK_QUEUE}")
    private val taskQueue: String,
    // A run that closed less than this ago counts as "just done": the enrichment event of the same import
    // may arrive after the import workflow already ran the artifacts, and must not regenerate them.
    @Value("\${temporal.artifacts.dedupe-window-seconds:300}")
    private val dedupeWindowSeconds: Long
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val WORKFLOW_ID_PREFIX = "geo-artifacts-"
        fun workflowIdFor(geoLocationId: Long) = "$WORKFLOW_ID_PREFIX$geoLocationId"
    }

    /**
     * Starts the workflow of [geoLocationId] and returns immediately.
     *
     * @param force start a new run even if one just closed (an explicit request); a run that is still
     *   in progress is never duplicated.
     * @return true when a new run was started, false when there was already a running/recent one.
     */
    fun start(geoLocationId: Long, force: Boolean = false): Boolean {
        val workflowId = workflowIdFor(geoLocationId)
        if (!force && runningOrRecentlyClosed(workflowId)) {
            logger.debug("Artifacts workflow {} already running or just finished; not starting another", workflowId)
            return false
        }
        val stub = workflowClient.newWorkflowStub(
            GeoLocationArtifactsWorkflow::class.java,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                // A new run is allowed after the previous one closed (re-import regenerates the artifacts).
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                .setMemo(mapOf("geoLocationId" to geoLocationId))
                .build()
        )
        return try {
            WorkflowClient.start(stub::generate, geoLocationId)
            logger.info("Started artifacts workflow {}", workflowId)
            true
        } catch (e: WorkflowExecutionAlreadyStarted) {
            logger.debug("Artifacts workflow {} was already running", workflowId)
            false
        }
    }

    /** Waits for the latest run of the workflow of [geoLocationId] and returns its result. */
    fun awaitResult(geoLocationId: Long, timeout: Duration): GeoLocationDetailReportResultDto =
        workflowClient.newUntypedWorkflowStub(workflowIdFor(geoLocationId))
            .getResult(timeout.toMillis(), TimeUnit.MILLISECONDS, GeoLocationDetailReportResultDto::class.java)

    private fun runningOrRecentlyClosed(workflowId: String): Boolean {
        val info = try {
            workflowClient.newUntypedWorkflowStub(workflowId).describe().workflowExecutionInfo
        } catch (e: WorkflowNotFoundException) {
            return false
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.NOT_FOUND) return false
            throw e
        }
        if (info.status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) return true
        // closeTime is a protobuf Timestamp, present only once the run has closed.
        if (!info.hasCloseTime()) return false
        val closedAt = Instant.ofEpochSecond(info.closeTime.seconds, info.closeTime.nanos.toLong())
        return closedAt.isAfter(Instant.now().minusSeconds(dedupeWindowSeconds))
    }
}
