package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag.LocalGeometricSvgFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary.LocalFallbackSummaryProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Wiring of [GeoLocationAIEnrichmentService] and [GeoLocationFlagService] onto [ProviderChain]:
 * sequential default, opt-in hedging, paid providers last, deterministic fallback.
 */
class EnrichmentServicesChainTest {

    private val place = mockk<IGeoLocationModel>(relaxed = true).also {
        every { it.name } returns "Germany"
        every { it.type } returns GeoLocationType.COUNTRY
        every { it.alias } returns "DEU"
        every { it.friendlyId } returns "DEU"
    }

    private class FakeSummary(
        override val providerId: String,
        override val order: Int,
        private val latencyMs: Long,
        private val answer: String?,
        override val hedgeable: Boolean = true
    ) : IGeoLocationSummaryProvider {
        val calls = AtomicInteger()
        override suspend fun getSummary(geoLocation: IGeoLocationModel, parentName: String?): String? {
            calls.incrementAndGet()
            delay(latencyMs)
            return answer
        }
    }

    private class FakeFlag(
        override val providerId: String,
        override val order: Int,
        private val latencyMs: Long,
        private val bytes: ByteArray?,
        override val hedgeable: Boolean = true
    ) : IGeoLocationFlagProvider {
        val calls = AtomicInteger()
        override suspend fun getFlag(geoLocation: IGeoLocationModel, parentName: String?) =
            run {
                calls.incrementAndGet()
                delay(latencyMs)
                bytes?.let {
                    GeoLocationFlagResult("flag", "desc", String(it), it, isSvg = true, mimeType = "image/svg+xml", sourceUrl = providerId)
                }
            }
    }

    private fun summaryService(hedging: Boolean, vararg providers: IGeoLocationSummaryProvider): GeoLocationAIEnrichmentService {
        val fallback = LocalFallbackSummaryProvider()
        return GeoLocationAIEnrichmentService(providers.toList() + fallback, fallback, hedging, 600, 6000)
    }

    private fun flagService(hedging: Boolean, vararg providers: IGeoLocationFlagProvider): GeoLocationFlagService {
        val fallback = LocalGeometricSvgFlagProvider()
        return GeoLocationFlagService(providers.toList() + fallback, fallback, hedging, 600, 6000)
    }

    @Test
    fun `summary - default is sequential and the paid provider comes last`() = runTest {
        val wikipedia = FakeSummary("WIKIPEDIA", 10, 1000, null)
        val countries = FakeSummary("REST_COUNTRIES", 20, 500, "from-countries")
        val gemini = FakeSummary("GEMINI_IA", 50, 100, "from-gemini", hedgeable = false)

        val result = summaryService(false, gemini, countries, wikipedia).enrichGeoLocationSuspend(place)

        assertEquals("from-countries", result)
        assertEquals(1500L, testScheduler.currentTime, "sequential: 1000 + 500")
        assertEquals(0, gemini.calls.get(), "no quota spent")
    }

    @Test
    fun `summary - with hedging the free providers overlap`() = runTest {
        val wikipedia = FakeSummary("WIKIPEDIA", 10, 1000, null)
        val countries = FakeSummary("REST_COUNTRIES", 20, 500, "from-countries")
        val gemini = FakeSummary("GEMINI_IA", 50, 100, "from-gemini", hedgeable = false)

        val result = summaryService(true, gemini, countries, wikipedia).enrichGeoLocationSuspend(place)

        assertEquals("from-countries", result)
        assertEquals(1100L, testScheduler.currentTime, "hedged: second starts at 600 and ends at 1100")
        assertEquals(0, gemini.calls.get())
    }

    @Test
    fun `summary - gemini is asked only after the free providers came up empty, then the local fallback`() = runTest {
        val wikipedia = FakeSummary("WIKIPEDIA", 10, 100, null)
        val gemini = FakeSummary("GEMINI_IA", 50, 100, "  from-gemini  ", hedgeable = false)

        assertEquals("from-gemini", summaryService(true, gemini, wikipedia).enrichGeoLocationSuspend(place))
        assertEquals(1, gemini.calls.get())

        val geminiDown = FakeSummary("GEMINI_IA", 50, 100, null, hedgeable = false)
        val fallbackText = summaryService(true, geminiDown, wikipedia).enrichGeoLocationSuspend(place)
        assertTrue(fallbackText.isNotBlank(), "deterministic local synthesis when everybody is out")
    }

    @Test
    fun `summary - blank answers do not count`() = runTest {
        val blank = FakeSummary("WIKIPEDIA", 10, 10, "   ")
        val countries = FakeSummary("REST_COUNTRIES", 20, 10, "real")

        assertEquals("real", summaryService(false, blank, countries).enrichGeoLocationSuspend(place))
    }

    @Test
    fun `flag - hedged chain returns the highest priority flag and skips the paid provider`() = runTest {
        val cdn = FakeFlag("FLAG_CDN", 10, 900, "<svg>cdn</svg>".toByteArray())
        val commons = FakeFlag("WIKIMEDIA", 20, 100, "<svg>commons</svg>".toByteArray())
        val gemini = FakeFlag("GEMINI_IA", 50, 100, "<svg>ai</svg>".toByteArray(), hedgeable = false)

        val flag = flagService(true, gemini, commons, cdn).obtainFlagSuspend(place)

        assertEquals("FLAG_CDN", flag.sourceUrl, "commons answered first (at 700) but the CDN has priority")
        assertEquals(900L, testScheduler.currentTime)
        assertEquals(0, gemini.calls.get())
    }

    @Test
    fun `flag - an empty payload is a miss and the local geometric flag is the last resort`() = runTest {
        val empty = FakeFlag("FLAG_CDN", 10, 10, ByteArray(0))

        val flag = flagService(false, empty).obtainFlagSuspend(place)

        assertTrue(flag.isSvg)
        assertEquals("image/svg+xml", flag.mimeType)
    }

    @Test
    fun `the blocking bridges give the same answer as the suspend functions`() {
        val countries = FakeSummary("REST_COUNTRIES", 20, 10, "bridge")

        assertEquals("bridge", summaryService(true, countries).enrichGeoLocation(place))
    }
}
