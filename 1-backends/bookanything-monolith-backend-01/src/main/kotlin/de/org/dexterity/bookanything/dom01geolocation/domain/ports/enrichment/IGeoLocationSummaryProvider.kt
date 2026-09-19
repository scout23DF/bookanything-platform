package de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel

/**
 * Interface defining a provider for geographic, demographic, and geopolitical text summaries.
 */
interface IGeoLocationSummaryProvider {
    val providerId: String
    val order: Int
    fun supports(geoLocation: IGeoLocationModel): Boolean = true
    fun getSummary(geoLocation: IGeoLocationModel, parentName: String?): String?
}
