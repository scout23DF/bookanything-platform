package de.org.dexterity.bookanything.dom01geolocation.application.workflow

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

    @ActivityMethod
    fun generateGeoLocationArtifactsAndReport(geoLocationId: Long): de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationDetailReportResultDto
}
