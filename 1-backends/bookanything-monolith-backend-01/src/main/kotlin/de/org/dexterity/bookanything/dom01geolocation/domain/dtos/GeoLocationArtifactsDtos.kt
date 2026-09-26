package de.org.dexterity.bookanything.dom01geolocation.domain.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Results of the steps of the per-GeoLocation artifacts workflow. They only carry ids, URLs and short
 * strings, never SVG/PDF content: activity inputs and outputs go through the Temporal workflow history
 * (2 MB per payload), so the heavy content stays in the storage and is re-read by the step that needs it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MapsStepResult(
    val geoLocationId: Long = 0L,
    val name: String = "",
    val type: String = "",
    val friendlyId: String = "",
    /** SUCCESS or SKIPPED_NO_GEOMETRY. */
    val status: String = "SUCCESS",
    val errorMessage: String? = null,
    val localMapAssetId: Long? = null,
    val localMapAssetUrl: String? = null,
    val worldHighlightAssetId: Long? = null,
    val worldHighlightAssetUrl: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlagStepResult(
    val flagAssetId: Long = 0L,
    val flagAssetUrl: String = "",
    val flagName: String = "",
    val flagDescription: String = "",
    val isSvg: Boolean = true
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReportStepResult(
    val reportAssetId: Long = 0L,
    val reportUrl: String = ""
)

/** Outcome of waiting for the artifact workflows of one country/level of a batch import. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArtifactsBatchSummary(
    val countrySlug: String = "",
    val locationLevel: Int = 0,
    val total: Int = 0,
    val succeeded: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val failedIds: List<Long> = emptyList()
)
