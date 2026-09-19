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
    val items: List<CountryImportItem> = emptyList(),
    val mapSvgGeneratorMechanism: String? = "KOTLIN"
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLocationMapsRequestDto(
    val geoLocationId: Long = 0L,
    val name: String = "",
    val type: String = "COUNTRY",
    val friendlyId: String = "",
    val alias: String? = null,
    val boundaryWkt: String = "",
    val saveToMinio: Boolean = true,
    val minioBucket: String? = "bookanything-images"
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLocationMapsResponseDto(
    val geoLocationId: Long = 0L,
    val friendlyId: String = "",
    val localMapSvg: String = "",
    val worldHighlightSvg: String = "",
    val localMapStorageKey: String? = null,
    val worldHighlightStorageKey: String? = null,
    val localMapUrl: String? = null,
    val worldHighlightUrl: String? = null,
    val status: String = "SUCCESS",
    val errorMessage: String? = null
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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLocationDetailReportResultDto(
    val geoLocationId: Long = 0L,
    val name: String = "",
    val type: String = "",
    val friendlyId: String = "",
    val localMapAssetId: Long? = null,
    val localMapAssetUrl: String? = null,
    val worldHighlightAssetId: Long? = null,
    val worldHighlightAssetUrl: String? = null,
    val flagAssetId: Long? = null,
    val flagAssetUrl: String? = null,
    val flagName: String? = null,
    val reportPdfUrl: String? = null,
    val aiEnrichmentSummary: String? = null,
    val status: String = "SUCCESS",
    val errorMessage: String? = null
)
