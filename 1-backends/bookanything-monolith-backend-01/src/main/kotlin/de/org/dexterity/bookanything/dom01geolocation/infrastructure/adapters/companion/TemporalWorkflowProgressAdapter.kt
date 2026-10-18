package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion

import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionWorkflow
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowPoll
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowProgressPort
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowRunState
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import io.temporal.api.enums.v1.WorkflowExecutionStatus
import io.temporal.client.WorkflowClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/**
 * Temporal's Java client is blocking, so every call is moved to [Dispatchers.IO] with `withContext`:
 * the coroutine suspends and the caller's thread is free while the query is on the wire.
 *
 * The workflow's progress query keeps answering after the workflow *failed* (with the last state it
 * had, still "RUNNING"), so `describe()` is asked too: it is the only source of "it is over, and badly".
 */
@Component
class TemporalWorkflowProgressAdapter(
    private val workflowClient: WorkflowClient
) : WorkflowProgressPort {

    private val io: CoroutineDispatcher = Dispatchers.IO

    override suspend fun poll(workflowId: String): WorkflowPoll = withContext(io) {
        val progress = workflowClient.newWorkflowStub(GeoLocationIngestionWorkflow::class.java, workflowId).getProgress()
        val status = workflowClient.newUntypedWorkflowStub(workflowId).describe().rawDescription.workflowExecutionInfo.status
        val state = when (status) {
            WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED -> WorkflowRunState.COMPLETED
            WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
            WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW -> WorkflowRunState.RUNNING
            else -> WorkflowRunState.FAILED
        }
        WorkflowPoll(progress, state, if (state == WorkflowRunState.FAILED) "Workflow ended as ${status.name.removePrefix("WORKFLOW_EXECUTION_STATUS_")}" else null)
    }

    override suspend fun result(workflowId: String): GeoLocationBatchImportResult = withContext(io) {
        workflowClient.newUntypedWorkflowStub(workflowId).getResult(GeoLocationBatchImportResult::class.java)
    }
}
