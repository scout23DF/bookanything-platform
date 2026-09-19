package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag

import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationFlagResult
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import kotlin.math.abs

@Component
@Order(999)
class LocalGeometricSvgFlagProvider : IGeoLocationFlagProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "LOCAL_FALLBACK"
    override val order: Int = 999

    override fun getFlag(geoLocation: IGeoLocationModel, parentName: String?): GeoLocationFlagResult {
        val name = geoLocation.name
        val type = geoLocation.type
        val code = geoLocation.alias ?: geoLocation.friendlyId

        logger.info("LocalGeometricSvgFlagProvider: Generating clean geometric fallback SVG for '$name' ($type)...")

        val hash = abs((name + code).hashCode())
        val colors = listOf(
            Pair("#1B365D", "#4B9CD3"),
            Pair("#002776", "#009C3B"),
            Pair("#2D68C4", "#F4C430"),
            Pair("#0047AB", "#E32636"),
            Pair("#107C41", "#FFD700"),
            Pair("#5C246E", "#E6A100")
        )
        val selectedColors = colors[hash % colors.size]

        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 600" width="900" height="600">
              <defs>
                <linearGradient id="flagGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="${selectedColors.first}"/>
                  <stop offset="100%" stop-color="${selectedColors.second}"/>
                </linearGradient>
              </defs>
              <rect width="900" height="600" fill="url(#flagGrad)"/>
              <rect x="0" y="240" width="900" height="120" fill="#FFFFFF" fill-opacity="0.9"/>
              <circle cx="450" cy="300" r="140" fill="#FFFFFF" stroke="${selectedColors.first}" stroke-width="8"/>
              <text x="450" y="290" font-family="Arial, sans-serif" font-size="38" font-weight="bold" fill="${selectedColors.first}" text-anchor="middle">$code</text>
              <text x="450" y="335" font-family="Arial, sans-serif" font-size="20" fill="#4B5563" text-anchor="middle">${type.name}</text>
            </svg>
        """.trimIndent()

        val bytes = svg.toByteArray(StandardCharsets.UTF_8)
        return GeoLocationFlagResult(
            flagName = "Estandarte Heráldico de $name",
            flagDescription = "Estandarte representativo institucional de $name ($code).",
            flagImageContent = svg,
            rawBytes = bytes,
            isSvg = true,
            mimeType = "image/svg+xml",
            sourceUrl = "local://geometric-fallback"
        )
    }
}
