package de.org.dexterity.bookanything.dom01geolocation.domain.dtos

import java.util.UUID

data class CountryDataToImportRequest(
    val countryIso3Code: String,
    val levels: List<Int> = listOf(0, 1),
    val parentAliasToAttach: String,
    val forceReimportIfExists: Boolean = false
)

data class GeoJsonDownloadRequest(
    val countryDataToImportRequestList: List<CountryDataToImportRequest>
)

data class GeoJsonDownloadResponse(
    val jobId: UUID,
    val message: String
)
