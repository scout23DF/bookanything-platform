package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.factories

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationEnrichmentFactory
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag.LocalGeometricSvgFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary.LocalFallbackSummaryProvider
import org.springframework.stereotype.Component

@Component
class LocalFallbackEnrichmentFactory(
    private val summaryProvider: LocalFallbackSummaryProvider,
    private val flagProvider: LocalGeometricSvgFlagProvider
) : IGeoLocationEnrichmentFactory {

    override val mechanismName: String = "LOCAL_FALLBACK"

    override fun createSummaryProvider(): IGeoLocationSummaryProvider = summaryProvider

    override fun createFlagProvider(): IGeoLocationFlagProvider = flagProvider
}
