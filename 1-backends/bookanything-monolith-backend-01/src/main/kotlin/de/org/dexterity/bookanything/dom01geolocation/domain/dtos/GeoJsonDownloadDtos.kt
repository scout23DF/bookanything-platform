package de.org.dexterity.bookanything.dom01geolocation.domain.dtos

import java.util.UUID

data class HierarchyDetailsRequest(
    val hierarchyType: String,
    val hierarchyLevelOfFileToImport: Int = 0,
    val parentAliasToAttach: String,
    val forceReimportIfExists: Boolean = false,
    val propertyForFieldFriendlyIdData: String? = null,
    val propertyForFieldNameData: String? = null,
    val propertyForFieldAliasData: String? = null,
    val propertyForSearchIfExists: String? = null,
    val propertyForParentSearch: String? = null
)

data class CountryDataToImportRequest(
    val countryIso3Code: String,
    val importingDetailsForCountry: HierarchyDetailsRequest? = null,
    val importingDetailsForProvince: HierarchyDetailsRequest? = null,
    val importingDetailsForCity: HierarchyDetailsRequest? = null,
    val importingDetailsForDistrict: HierarchyDetailsRequest? = null
)

data class GeoJsonDownloadRequest(
    val countryDataToImportRequestList: List<CountryDataToImportRequest>
)

data class GeoJsonDownloadResponse(
    val jobId: UUID,
    val message: String
)
