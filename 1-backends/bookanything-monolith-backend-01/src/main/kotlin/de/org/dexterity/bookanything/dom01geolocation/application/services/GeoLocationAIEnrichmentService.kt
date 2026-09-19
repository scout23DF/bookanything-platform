package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary.LocalFallbackSummaryProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service orchestrating GeoLocation demographic, geopolitical, and historical text summaries
 * across a pluggable Chain of Responsibility of providers (Wikipedia, OpenData/RESTCountries, Gemini AI, Fallback).
 */
@Service
class GeoLocationAIEnrichmentService(
    private val summaryProviders: List<IGeoLocationSummaryProvider>,
    private val fallbackProvider: LocalFallbackSummaryProvider
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Enriches a GeoLocation with factual demographic, geopolitical, tourist,
     * and historical facts using the prioritized pluggable provider chain.
     */
    fun enrichGeoLocation(geoLocation: IGeoLocationModel, parentName: String? = null): String {
        val name = geoLocation.name
        val type = geoLocation.type.name
        val sortedProviders = summaryProviders.sortedBy { it.order }

        logger.info("Summary Pipeline: Starting summary resolution for '$name' ($type) across ${sortedProviders.size} registered providers...")

        for (provider in sortedProviders) {
            if (!provider.supports(geoLocation)) {
                logger.debug("Summary Pipeline: Provider '${provider.providerId}' does not support entity '$name' ($type). Skipping.")
                continue
            }

            val startTime = System.currentTimeMillis()
            try {
                logger.info("Summary Pipeline: Attempting resolution via [${provider.providerId}] for '$name' ($type)...")
                val summary = provider.getSummary(geoLocation, parentName)
                val duration = System.currentTimeMillis() - startTime

                if (!summary.isNullOrBlank()) {
                    logger.info("Summary Pipeline: [${provider.providerId}] successfully resolved summary for '$name' in ${duration}ms (${summary.length} chars)")
                    return summary.trim().take(2000)
                } else {
                    logger.warn("Summary Pipeline: [${provider.providerId}] returned empty result for '$name' in ${duration}ms. Failing over to next provider in chain...")
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val rootCause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(e)
                logger.warn("Summary Pipeline: [${provider.providerId}] threw exception for '$name' after ${duration}ms: [${rootCause.javaClass.simpleName}] ${rootCause.message}. Failing over to next provider...")
            }
        }

        logger.warn("Summary Pipeline: All external providers exhausted for '$name'. Falling back to deterministic local synthesis.")
        return fallbackProvider.getSummary(geoLocation, parentName)
    }
}
