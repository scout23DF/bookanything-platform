package de.org.dexterity.bookanything.dom01geolocation.domain.events

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonDownloadRequest
import java.util.UUID

/**
 * Event published when a user requests to download a batch of GeoJSON files.
 * This is the entry point for the asynchronous download process.
 */
data class GeoJsonDownloadRequestedEvent(
    val jobId: UUID,
    val request: GeoJsonDownloadRequest
)

/**
 * Event published by the dispatcher for each individual country and level to be downloaded.
 * This event triggers a download worker.
 */
data class CountryGeoJsonDataRequiredEvent(
    val jobId: UUID,
    val countryIso3Code: String,
    val level: Int,
    val parentAliasToAttach: String,
    val forceReimportIfExists: Boolean = false
)

/**
 * Event published after a GeoJSON file has been successfully downloaded and stored temporarily.
 * This event triggers the upload worker.
 *
 * @param tempFilePath The local path to the downloaded file.
 * @param fileName The name of the file that should be used for storage (e.g., in Minio).
 */
data class GeoJsonFileDownloadedEvent(
    val jobId: UUID,
    val countryIso3Code: String,
    val level: Int,
    val tempFilePath: String,
    val fileName: String,
    val parentAliasToAttach: String,
    val forceReimportIfExists: Boolean = false
)

/**
 * Event published when any step of the download or processing fails for a specific file.
 */
data class GeoJsonDownloadFailedEvent(
    val jobId: UUID,
    val countryIso3Code: String,
    val level: Int,
    val reason: String,
    val parentAliasToAttach: String,
    val forceReimportIfExists: Boolean = false
)

/**
 * Event published when a new GeoJSON file and its Features have been successfully imported into the database.
 */
data class CountryDataToMakeGeoLocationsEvent(
    val geoJsonImportedFileId: UUID,
    val parentAliasToAttach: String,
    val forceReimportIfExists: Boolean = false
)
