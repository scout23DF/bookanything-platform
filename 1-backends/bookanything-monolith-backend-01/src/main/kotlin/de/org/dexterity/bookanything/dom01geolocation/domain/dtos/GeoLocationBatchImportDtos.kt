package de.org.dexterity.bookanything.dom01geolocation.domain.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CountryImportItem(
    val countrySlug: String = "",
    val locationLevel: Int = 0,
    val shouldConvertToXML: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLocationBatchImportRequest(
    val items: List<CountryImportItem> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImportProgressDto(
    val currentStage: String = "INITIALIZING",
    val percentCompleted: Int = 0,
    val processedItems: Int = 0,
    val totalItems: Int = 0,
    val status: String = "RUNNING",
    val details: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CountryImportSummaryDto(
    val countrySlug: String = "",
    val locationLevel: Int = 0,
    val rawGeoJsonUrl: String? = null,
    val convertedXmlUrl: String? = null,
    val createdCount: Int = 0,
    val updatedCount: Int = 0,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLocationBatchImportResult(
    val jobId: String = "",
    val totalCreated: Int = 0,
    val totalUpdated: Int = 0,
    val reportPdfUrl: String? = null,
    val countrySummaries: List<CountryImportSummaryDto> = emptyList(),
    val status: String = "COMPLETED"
)
