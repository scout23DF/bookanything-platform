package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary.LocalFallbackSummaryProvider
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service orchestrating GeoLocation demographic, geopolitical, and historical text summaries
 * across a pluggable Chain of Responsibility of providers (Wikipedia, OpenData/RESTCountries, Gemini AI, Fallback).
 *
 * The chain is sequential by default. With `enrichment.hedging.enabled=true` the free providers on
 * different hosts run hedged (see [ProviderChain]); Gemini is still only asked after them.
 */
@Service
class GeoLocationAIEnrichmentService(
    private val summaryProviders: List<IGeoLocationSummaryProvider>,
    private val fallbackProvider: LocalFallbackSummaryProvider,
    @Value("\${application.domain-settings.geolocation.enrichment.hedging.enabled:false}")
    hedgingEnabled: Boolean,
    @Value("\${application.domain-settings.geolocation.enrichment.hedging.hedge-delay-ms:600}")
    hedgeDelayMs: Long,
    @Value("\${application.domain-settings.geolocation.enrichment.hedging.provider-budget-ms:6000}")
    providerBudgetMs: Long
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val chain = ProviderChain("Summary", hedgingEnabled, hedgeDelayMs, providerBudgetMs)

    /**
     * Enriches a GeoLocation with factual demographic, geopolitical, tourist,
     * and historical facts using the prioritized pluggable provider chain.
     */
    suspend fun enrichGeoLocationSuspend(geoLocation: IGeoLocationModel, parentName: String? = null): String {
        val name = geoLocation.name
        val type = geoLocation.type.name
        val candidates = summaryProviders
            .filter { it !== fallbackProvider && it.supports(geoLocation) }
            .sortedBy { it.order }

        logger.info("Summary Pipeline: Starting summary resolution for '$name' ($type) across ${candidates.size} supporting providers...")

        val summary = chain.resolve(
            providers = candidates,
            idOf = { it.providerId },
            hedgeable = { it.hedgeable }
        ) { provider -> provider.getSummary(geoLocation, parentName)?.takeIf { it.isNotBlank() } }

        if (summary != null) return summary.trim().take(2000)

        logger.warn("Summary Pipeline: All external providers exhausted for '$name'. Falling back to deterministic local synthesis.")
        return fallbackProvider.getSummary(geoLocation, parentName)
    }

    /** Blocking bridge for callers that are not `suspend` (Temporal activities). Prefer [enrichGeoLocationSuspend]. */
    fun enrichGeoLocation(geoLocation: IGeoLocationModel, parentName: String? = null): String =
        runBlocking { enrichGeoLocationSuspend(geoLocation, parentName) }
}
