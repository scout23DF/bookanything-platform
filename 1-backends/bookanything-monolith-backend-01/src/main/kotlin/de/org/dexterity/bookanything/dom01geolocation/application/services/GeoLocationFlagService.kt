package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag.LocalGeometricSvgFlagProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class GeoLocationFlagResult(
    val flagName: String,
    val flagDescription: String,
    val flagImageContent: String, // inline SVG or "data:image/png;base64,..."
    val rawBytes: ByteArray,
    val isSvg: Boolean,
    val mimeType: String,
    val sourceUrl: String? = null
)

/**
 * Service orchestrating official vector flags and heraldic assets
 * across a pluggable Chain of Responsibility of providers (FlagCDN, Wikimedia Commons, Gemini AI, Fallback).
 */
@Service
class GeoLocationFlagService(
    private val flagProviders: List<IGeoLocationFlagProvider>,
    private val fallbackProvider: LocalGeometricSvgFlagProvider
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Resolves the official vector flag for a GeoLocation using the prioritized pluggable provider chain.
     */
    fun obtainFlag(geoLocation: IGeoLocationModel, parentName: String? = null): GeoLocationFlagResult {
        val name = geoLocation.name
        val type = geoLocation.type.name
        val sortedProviders = flagProviders.sortedBy { it.order }

        logger.info("Flag Pipeline: Starting flag resolution for '$name' ($type) across ${sortedProviders.size} registered providers...")

        for (provider in sortedProviders) {
            if (!provider.supports(geoLocation)) {
                logger.debug("Flag Pipeline: Provider '${provider.providerId}' does not support entity '$name' ($type). Skipping.")
                continue
            }

            val startTime = System.currentTimeMillis()
            try {
                logger.info("Flag Pipeline: Attempting resolution via [${provider.providerId}] for '$name' ($type)...")
                val result = provider.getFlag(geoLocation, parentName)
                val duration = System.currentTimeMillis() - startTime

                if (result != null && result.rawBytes.isNotEmpty()) {
                    logger.info("Flag Pipeline: [${provider.providerId}] successfully resolved flag for '$name' in ${duration}ms (MIME: ${result.mimeType}, Source: ${result.sourceUrl})")
                    return result
                } else {
                    logger.warn("Flag Pipeline: [${provider.providerId}] returned null/empty flag for '$name' in ${duration}ms. Failing over to next provider in chain...")
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val rootCause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(e)
                logger.warn("Flag Pipeline: [${provider.providerId}] threw exception for '$name' after ${duration}ms: [${rootCause.javaClass.simpleName}] ${rootCause.message}. Failing over to next provider...")
            }
        }

        logger.warn("Flag Pipeline: All external providers exhausted for '$name'. Falling back to deterministic local geometric SVG banner.")
        return fallbackProvider.getFlag(geoLocation, parentName)
    }
}
