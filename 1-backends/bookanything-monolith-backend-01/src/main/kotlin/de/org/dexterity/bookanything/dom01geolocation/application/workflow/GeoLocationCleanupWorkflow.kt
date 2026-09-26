package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationCleanupRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationCleanupResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

/** Reverses the import of countries/levels: see ADR-0015. Dry run by default. */
@WorkflowInterface
interface GeoLocationCleanupWorkflow {

    @WorkflowMethod
    fun executeCleanup(request: GeoLocationCleanupRequest): GeoLocationCleanupResult

    @QueryMethod
    fun getProgress(): ImportProgressDto
}
