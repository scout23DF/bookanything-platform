package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag

import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationFlagResult
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.http.ResilientHttpFetcher
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Locale

@Component
@Order(10)
class FlagCdnProvider(
    private val http: ResilientHttpFetcher
) : IGeoLocationFlagProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "FLAG_CDN"
    override val order: Int = 10

    override fun supports(geoLocation: IGeoLocationModel): Boolean {
        return geoLocation.type == GeoLocationType.COUNTRY
    }

    override fun getFlag(geoLocation: IGeoLocationModel, parentName: String?): GeoLocationFlagResult? {
        val name = geoLocation.name
        val code = geoLocation.alias ?: geoLocation.friendlyId
        val directIso = resolveIso2(code) ?: return null

        val flagCdnSvgUrl = "https://flagcdn.com/$directIso.svg"
        logger.info("FlagCdnProvider: Fetching vector SVG for '$name' ($directIso) from $flagCdnSvgUrl...")

        val svgBytes = downloadBytes(flagCdnSvgUrl) ?: return null
        val svgContent = String(svgBytes, StandardCharsets.UTF_8)

        if (!isValidSvg(svgContent)) {
            logger.warn("FlagCdnProvider: Downloaded payload from $flagCdnSvgUrl is not a valid SVG")
            return null
        }

        logger.info("FlagCdnProvider: Successfully retrieved authoritative SVG for '$name' (${svgBytes.size} bytes)")
        return GeoLocationFlagResult(
            flagName = "Bandeira Oficial de $name",
            flagDescription = "Bandeira soberana nacional de $name ($code).",
            flagImageContent = svgContent,
            rawBytes = svgBytes,
            isSvg = true,
            mimeType = "image/svg+xml",
            sourceUrl = flagCdnSvgUrl
        )
    }

    private fun resolveIso2(code: String): String? {
        val clean = code.trim().uppercase(Locale.ROOT)
        val iso2 = when {
            clean.length == 2 && clean.all { it.isLetter() } -> clean
            clean.length == 3 -> ISO3_TO_ISO2[clean]
            else -> null
        }
        return iso2?.lowercase(Locale.ROOT)
    }

    private fun downloadBytes(url: String): ByteArray? = http.getBytes(url)

    companion object {
        // Full ISO 3166-1 alpha-3 -> alpha-2 table from the JDK (was a hand-written 17-country list).
        private val ISO3_TO_ISO2: Map<String, String> = Locale.getISOCountries().mapNotNull { iso2 ->
            runCatching { Locale.of("", iso2).isO3Country }.getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { it.uppercase(Locale.ROOT) to iso2 }
        }.toMap()
    }

    private fun isValidSvg(content: String): Boolean {
        val trimmed = content.trim()
        return (trimmed.startsWith("<svg", ignoreCase = true) || trimmed.contains("<svg", ignoreCase = true)) &&
                trimmed.contains("</svg>", ignoreCase = true)
    }
}
