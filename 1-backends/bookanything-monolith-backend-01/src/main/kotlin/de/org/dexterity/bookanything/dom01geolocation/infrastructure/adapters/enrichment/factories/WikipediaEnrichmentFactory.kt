package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.factories

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationEnrichmentFactory
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag.WikimediaCommonsFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary.WikipediaSummaryProvider
import org.springframework.stereotype.Component

@Component
class WikipediaEnrichmentFactory(
    private val summaryProvider: WikipediaSummaryProvider,
    private val flagProvider: WikimediaCommonsFlagProvider
) : IGeoLocationEnrichmentFactory {

    override val mechanismName: String = "WIKIPEDIA"

    override fun createSummaryProvider(): IGeoLocationSummaryProvider = summaryProvider

    override fun createFlagProvider(): IGeoLocationFlagProvider = flagProvider
}
