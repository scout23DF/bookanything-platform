package de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment

/**
 * Abstract Factory defining the creation contract for a family of GeoLocation enrichment providers
 * (text summaries and vector flags).
 */
interface IGeoLocationEnrichmentFactory {
    val mechanismName: String
    fun createSummaryProvider(): IGeoLocationSummaryProvider
    fun createFlagProvider(): IGeoLocationFlagProvider
}
