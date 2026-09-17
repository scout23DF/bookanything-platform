package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface GeoLocationIngestionWorkflow {

    @WorkflowMethod
    fun executeBatchImport(request: GeoLocationBatchImportRequest): GeoLocationBatchImportResult

    @QueryMethod
    fun getProgress(): ImportProgressDto
}
