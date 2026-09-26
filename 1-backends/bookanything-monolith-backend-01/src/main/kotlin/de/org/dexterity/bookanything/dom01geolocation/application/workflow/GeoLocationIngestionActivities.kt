package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ArtifactsBatchSummary
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportSummaryDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

@ActivityInterface
interface GeoLocationIngestionActivities {

    @ActivityMethod
    fun processCountryLocationViaNiFi(item: CountryImportItem): CountryImportSummaryDto

    @ActivityMethod
    fun generateJsReportSummaryPdf(jobId: String, summaries: List<CountryImportSummaryDto>): String

    @ActivityMethod
    fun publishBatchImportCompletedEvent(result: GeoLocationBatchImportResult)

    /**
     * Waits for the artifacts of every GeoLocation of the country/level (ids read from the database),
     * starting any per-item workflow that is missing (ADR-0014). Heartbeats while waiting.
     */
    @ActivityMethod
    fun awaitArtifactWorkflows(item: CountryImportItem): ArtifactsBatchSummary

    @ActivityMethod
    fun configureMapSvgGeneratorMechanism(mechanism: String)
}
