package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportRequest
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

/** Starts the batch-import workflow asynchronously; shared by the REST endpoint and the Companion. */
@Component
class BatchImportStarter(
    private val workflowClient: WorkflowClient,
    @Value("\${temporal.task-queue:GEOLOCATION_INGESTION_TASK_QUEUE}")
    val taskQueue: String
) {
    /** @return the id of the workflow that was started */
    fun start(request: GeoLocationBatchImportRequest): String {
        val workflowId = "geo-import-${UUID.randomUUID()}"
        val options = WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(taskQueue).build()
        val stub = workflowClient.newWorkflowStub(GeoLocationIngestionWorkflow::class.java, options)
        WorkflowClient.start(stub::executeBatchImport, request)
        return workflowId
    }
}
