package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.factories

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationEnrichmentFactory
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag.FlagCdnProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary.RestCountriesSummaryProvider
import org.springframework.stereotype.Component

@Component
class OpenDataEnrichmentFactory(
    private val summaryProvider: RestCountriesSummaryProvider,
    private val flagProvider: FlagCdnProvider
) : IGeoLocationEnrichmentFactory {

    override val mechanismName: String = "OPEN_DATA"

    override fun createSummaryProvider(): IGeoLocationSummaryProvider = summaryProvider

    override fun createFlagProvider(): IGeoLocationFlagProvider = flagProvider
}
