package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.factories

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationEnrichmentFactory
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag.GeminiAiFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary.GeminiAiSummaryProvider
import org.springframework.stereotype.Component

@Component
class GeminiAiEnrichmentFactory(
    private val summaryProvider: GeminiAiSummaryProvider,
    private val flagProvider: GeminiAiFlagProvider
) : IGeoLocationEnrichmentFactory {

    override val mechanismName: String = "GEMINI_IA"

    override fun createSummaryProvider(): IGeoLocationSummaryProvider = summaryProvider

    override fun createFlagProvider(): IGeoLocationFlagProvider = flagProvider
}
