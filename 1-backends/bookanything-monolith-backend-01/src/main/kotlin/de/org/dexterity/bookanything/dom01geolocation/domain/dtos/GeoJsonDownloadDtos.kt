package de.org.dexterity.bookanything.dom01geolocation.domain.dtos

import java.util.UUID

/**
 * Data Transfer Object for initiating a GeoJSON download process.
 *
 * @property countryIso3Codes A list of uppercase, 3-letter country ISO codes (e.g., "BRA", "USA").
 * @property levels A list of detail levels to download for each country. Levels can range from 0 to 4.
 *                  If not provided, defaults to levels 0 and 1.
 */
data class GeoJsonDownloadRequest(
    val countryIso3Codes: List<String>,
    val levels: List<Int> = listOf(0, 1)
)

/**
 * Data Transfer Object for the response after a download job has been accepted.
 *
 * @property jobId A unique identifier for the asynchronous download job.
 * @property message A confirmation message for the user.
 */
data class GeoJsonDownloadResponse(
    val jobId: UUID,
    val message: String
)
