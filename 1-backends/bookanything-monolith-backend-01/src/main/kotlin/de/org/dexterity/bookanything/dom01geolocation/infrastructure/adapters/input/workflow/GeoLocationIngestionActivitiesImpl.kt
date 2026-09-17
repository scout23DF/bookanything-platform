package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.workflow

import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionActivities
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportSummaryDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GeoLocationIngestionActivitiesImpl(
    private val eventPublisher: EventPublisherPort,
    @Value("\${nifi.base-url:https://nifi.darueira-corpshared.127.0.0.1.nip.io}")
    private val nifiBaseUrl: String,
    @Value("\${jsreport.base-url:http://10.152.183.35:5488}")
    private val jsreportBaseUrl: String
) : GeoLocationIngestionActivities {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun processCountryLocationViaNiFi(item: CountryImportItem): CountryImportSummaryDto {
        val startTime = System.currentTimeMillis()
        logger.info("Temporal Activity: Starting NiFi Ingestion for ${item.countrySlug} (Level ${item.locationLevel}) [XML=${item.shouldConvertToXML}] at $nifiBaseUrl")

        // In Etapa 2, this will invoke the NiFi Dataflow trigger endpoint/webhook.
        // For now, placeholder returning structure with duration.
        val duration = System.currentTimeMillis() - startTime

        return CountryImportSummaryDto(
            countrySlug = item.countrySlug,
            locationLevel = item.locationLevel,
            rawGeoJsonUrl = "s3://darueira-geodata/raw/gadm41_${item.countrySlug}_${item.locationLevel}.json",
            convertedXmlUrl = if (item.shouldConvertToXML) "s3://darueira-geodata/xml/gadm41_${item.countrySlug}_${item.locationLevel}.xml" else null,
            createdCount = 1,
            updatedCount = 0,
            durationMs = duration
        )
    }

    override fun generateJsReportSummaryPdf(jobId: String, summaries: List<CountryImportSummaryDto>): String {
        logger.info("Temporal Activity: Requesting JSReport PDF generation for Job $jobId at $jsreportBaseUrl (Items: ${summaries.size})")

        // In Etapa 3, this will call JSReport REST API /api/report to render the PDF template
        // and upload the resulting bytes to MinIO bucket.
        val pdfObjectKey = "s3://darueira-reports/geolocations/job-${jobId}-summary.pdf"
        logger.info("Temporal Activity: Generated report artifact at $pdfObjectKey")
        return pdfObjectKey
    }

    override fun publishBatchImportCompletedEvent(result: GeoLocationBatchImportResult) {
        logger.info("Temporal Activity: Publishing completion event to Kafka for Job ${result.jobId} (Total Created: ${result.totalCreated}, Total Updated: ${result.totalUpdated})")
        // Will publish to topics.geolocation.batch-import.completed
    }
}
