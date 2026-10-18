package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag.LocalGeometricSvgFlagProvider
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
 *
 * The chain is sequential by default. With `enrichment.hedging.enabled=true` the free providers on
 * different hosts run hedged (see [ProviderChain]); Gemini is still only asked after them.
 */
@Service
class GeoLocationFlagService(
    private val flagProviders: List<IGeoLocationFlagProvider>,
    private val fallbackProvider: LocalGeometricSvgFlagProvider,
    @Value("\${application.domain-settings.geolocation.enrichment.hedging.enabled:false}")
    hedgingEnabled: Boolean,
    @Value("\${application.domain-settings.geolocation.enrichment.hedging.hedge-delay-ms:600}")
    hedgeDelayMs: Long,
    @Value("\${application.domain-settings.geolocation.enrichment.hedging.provider-budget-ms:6000}")
    providerBudgetMs: Long
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val chain = ProviderChain("Flag", hedgingEnabled, hedgeDelayMs, providerBudgetMs)

    /**
     * Resolves the official vector flag for a GeoLocation using the prioritized pluggable provider chain.
     */
    suspend fun obtainFlagSuspend(geoLocation: IGeoLocationModel, parentName: String? = null): GeoLocationFlagResult {
        val name = geoLocation.name
        val type = geoLocation.type.name
        val candidates = flagProviders
            .filter { it !== fallbackProvider && it.supports(geoLocation) }
            .sortedBy { it.order }

        logger.info("Flag Pipeline: Starting flag resolution for '$name' ($type) across ${candidates.size} supporting providers...")

        val flag = chain.resolve(
            providers = candidates,
            idOf = { it.providerId },
            hedgeable = { it.hedgeable }
        ) { provider -> provider.getFlag(geoLocation, parentName)?.takeIf { it.rawBytes.isNotEmpty() } }

        if (flag != null) return flag

        logger.warn("Flag Pipeline: All external providers exhausted for '$name'. Falling back to deterministic local geometric SVG banner.")
        return fallbackProvider.getFlag(geoLocation, parentName)
    }

    /** Blocking bridge for callers that are not `suspend` (Temporal activities). Prefer [obtainFlagSuspend]. */
    fun obtainFlag(geoLocation: IGeoLocationModel, parentName: String? = null): GeoLocationFlagResult =
        runBlocking { obtainFlagSuspend(geoLocation, parentName) }
}
