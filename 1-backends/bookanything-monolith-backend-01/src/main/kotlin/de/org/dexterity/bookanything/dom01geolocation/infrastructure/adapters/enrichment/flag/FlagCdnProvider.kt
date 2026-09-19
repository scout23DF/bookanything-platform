package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag

import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationFlagResult
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Locale

@Component
@Order(10)
class FlagCdnProvider(
    private val webClient: WebClient
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
        val iso2 = when (clean) {
            "BRA" -> "br"
            "DEU" -> "de"
            "USA" -> "us"
            "ARG" -> "ar"
            "FRA" -> "fr"
            "GBR" -> "gb"
            "ESP" -> "es"
            "ITA" -> "it"
            "PRT" -> "pt"
            "JPN" -> "jp"
            "CAN" -> "ca"
            "MEX" -> "mx"
            "AUS" -> "au"
            "CHN" -> "cn"
            "IND" -> "in"
            "RUS" -> "ru"
            "ZAF" -> "za"
            else -> if (clean.length == 2) clean.lowercase(Locale.ROOT) else null
        }
        return if (iso2 != null && iso2.length == 2 && iso2.all { it.isLetter() }) iso2 else null
    }

    private fun downloadBytes(url: String): ByteArray? {
        return try {
            val uri = URI.create(url)
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .timeout(Duration.ofMillis(3500))
                .block()
        } catch (e: Exception) {
            logger.debug("FlagCdnProvider: Could not download from $url: ${e.message}")
            null
        }
    }

    private fun isValidSvg(content: String): Boolean {
        val trimmed = content.trim()
        return (trimmed.startsWith("<svg", ignoreCase = true) || trimmed.contains("<svg", ignoreCase = true)) &&
                trimmed.contains("</svg>", ignoreCase = true)
    }
}
