package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.FlagStepResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.MapsStepResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ReportStepResult
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

/**
 * Steps of [GeoLocationArtifactsWorkflow]. Each step persists its output (assets, entity details) and
 * returns only ids/URLs/short strings. Failures are thrown so Temporal retries them; only the business
 * outcome "no geometry" is returned.
 */
@ActivityInterface
interface GeoLocationArtifactsActivities {

    /** Generates the local map and the world-highlight SVGs and stores them in the tenant MinIO. */
    @ActivityMethod
    fun generateAndStoreMaps(geoLocationId: Long): MapsStepResult

    /** Resolves the official flag and stores it in the tenant MinIO. */
    @ActivityMethod
    fun resolveAndStoreFlag(geoLocationId: Long): FlagStepResult

    /** Asks the AI enrichment service for the summary and persists it in the entity details. */
    @ActivityMethod
    fun enrichWithAi(geoLocationId: Long): String

    /** Renders the detail PDF in JSReport from the stored SVGs/flag and stores it in the tenant MinIO. */
    @ActivityMethod
    fun renderAndStoreReport(
        geoLocationId: Long,
        maps: MapsStepResult,
        flag: FlagStepResult,
        aiSummary: String
    ): ReportStepResult

    /** Copies the stored detail PDF to the corporate MinIO reports bucket; returns the corporate URL. */
    @ActivityMethod
    fun copyReportToCorporateStorage(geoLocationId: Long, reportAssetId: Long): String
}
