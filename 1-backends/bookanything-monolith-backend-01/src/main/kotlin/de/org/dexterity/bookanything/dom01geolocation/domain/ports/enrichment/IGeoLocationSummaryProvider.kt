package de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel

/**
 * Interface defining a provider for geographic, demographic, and geopolitical text summaries.
 */
interface IGeoLocationSummaryProvider {
    val providerId: String
    val order: Int
    fun supports(geoLocation: IGeoLocationModel): Boolean = true
    /**
     * False for providers with a cost or a hard quota (Gemini): the chain only asks them after the
     * free ones came up empty, never in parallel with them.
     */
    val hedgeable: Boolean get() = true

    suspend fun getSummary(geoLocation: IGeoLocationModel, parentName: String?): String?
}
