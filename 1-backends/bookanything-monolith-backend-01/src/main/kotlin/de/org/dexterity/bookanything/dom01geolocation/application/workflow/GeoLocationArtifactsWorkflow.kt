package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationDetailReportResultDto
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

/**
 * Generates the artifacts of ONE GeoLocation: SVG maps, flag, AI summary, detail PDF and its corporate
 * copy. Workflow id: `geo-artifacts-<geoLocationId>` (see [GeoLocationArtifactsLauncher]). See ADR-0014.
 */
@WorkflowInterface
interface GeoLocationArtifactsWorkflow {

    @WorkflowMethod
    fun generate(geoLocationId: Long): GeoLocationDetailReportResultDto

    /** Name of the step being executed (MAPS, FLAG, AI_SUMMARY, REPORT, CORPORATE_COPY, DONE). */
    @QueryMethod
    fun getCurrentStep(): String
}
