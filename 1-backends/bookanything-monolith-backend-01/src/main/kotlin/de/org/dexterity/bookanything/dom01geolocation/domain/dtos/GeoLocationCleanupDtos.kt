package de.org.dexterity.bookanything.dom01geolocation.domain.dtos

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/** One country/level to clean up. Level 0 is the country itself, level 1 its provinces (ADR-0015). */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CleanupItem(
    val countrySlug: String = "",
    val locationLevel: Int = 0,
    // Nullable on purpose: in the native image Jackson cannot use the Kotlin default of a missing property and
    // would pass null to a primitive boolean (the request failed to parse). A missing flag means "do not delete".
    val shouldDeleteJsonFile: Boolean? = false,
    val shouldDeleteXmlFile: Boolean? = false
) {
    @get:JsonIgnore
    val deleteJson: Boolean get() = shouldDeleteJsonFile == true

    @get:JsonIgnore
    val deleteXml: Boolean get() = shouldDeleteXmlFile == true
}

/** `dryRun` defaults to true: nothing is deleted unless the caller sends `dryRun=false` explicitly. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLocationCleanupRequest(
    val items: List<CleanupItem> = emptyList(),
    // Nullable on purpose (see CleanupItem). A missing or null dryRun MUST mean true: only an explicit false deletes.
    val dryRun: Boolean? = true
) {
    /**
     * A function on purpose: a Kotlin `val isDryRun` compiles to the getter `isDryRun()`, which Jackson reads as the
     * JSON property `dryRun` (the same as the field) and an @JsonIgnore on it would drop `dryRun` altogether.
     */
    fun dryRunEnabled(): Boolean = dryRun != false
}

/** What a cleanup of one item would delete (read only). */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CleanupItemPlan(
    val countrySlug: String = "",
    val locationLevel: Int = 0,
    /** COUNTRY for level 0, PROVINCE for level 1. */
    val targetType: String = "",
    val geoLocationIds: List<Long> = emptyList(),
    val assetCount: Int = 0,
    val corporateReportCount: Int = 0,
    /** Provinces of the country in the database (relevant for level 0). */
    val childProvinceCount: Int = 0,
    val jsonFileExists: Boolean = false,
    val xmlFileExists: Boolean = false,
    val willDeleteJsonFile: Boolean = false,
    val willDeleteXmlFile: Boolean = false,
    val blockers: List<String> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CleanupDeletionSummary(
    val countrySlug: String = "",
    val locationLevel: Int = 0,
    val deletedGeoLocations: Int = 0,
    val deletedAssets: Int = 0,
    val deletedCorporateReports: Int = 0,
    val errorMessage: String? = null
)

/** Reply of the NiFi cleanup flow (topic geolocation.nifi-cleanup.completed). */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NiFiCleanupSummaryDto(
    val jobId: String = "",
    val countrySlug: String = "",
    val locationLevel: Int = 0,
    val deletedJson: Boolean = false,
    val deletedXml: Boolean = false,
    val failedCount: Int = 0,
    val durationMs: Long = 0,
    val errorMessage: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CleanupItemResult(
    val countrySlug: String = "",
    val locationLevel: Int = 0,
    val deletedGeoLocations: Int = 0,
    val deletedAssets: Int = 0,
    val deletedCorporateReports: Int = 0,
    val deletedJsonFile: Boolean = false,
    val deletedXmlFile: Boolean = false,
    /** GeoLocations of the item still in the database after the cleanup (0 when it worked). */
    val remainingGeoLocations: Int = 0,
    val remainingAssets: Int = 0,
    val errorMessage: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLocationCleanupResult(
    val jobId: String = "",
    val dryRun: Boolean = true,
    /** DRY_RUN, BLOCKED, COMPLETED or COMPLETED_WITH_ERRORS. */
    val status: String = "DRY_RUN",
    val plans: List<CleanupItemPlan> = emptyList(),
    val results: List<CleanupItemResult> = emptyList(),
    val blockers: List<String> = emptyList()
)
