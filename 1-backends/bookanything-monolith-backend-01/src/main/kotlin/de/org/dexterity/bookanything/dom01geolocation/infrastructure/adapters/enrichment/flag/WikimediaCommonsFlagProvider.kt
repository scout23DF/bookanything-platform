package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationFlagResult
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.http.ResilientHttpFetcher
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.wikidata.WikidataGeoResolver
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@Component
@Order(20)
class WikimediaCommonsFlagProvider(
    private val http: ResilientHttpFetcher,
    private val wikidata: WikidataGeoResolver,
    private val objectMapper: ObjectMapper
) : IGeoLocationFlagProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "WIKIMEDIA"
    override val order: Int = 20

    // Offline backup for when Wikidata is unreachable: Commons file names only. The upload URL
    // (including its MD5 hash directories) is computed by WikidataGeoResolver.commonsFileUrl;
    // hand-copied URLs had wrong hash directories for 12 of these 43 flags.
    private val brazilStateFlagFiles = mapOf(
        "BR-AC" to "Bandeira_do_Acre.svg",
        "BR-AL" to "Bandeira_de_Alagoas.svg",
        "BR-AP" to "Bandeira_do_Amapá.svg",
        "BR-AM" to "Bandeira_do_Amazonas.svg",
        "BR-BA" to "Bandeira_da_Bahia.svg",
        "BR-CE" to "Bandeira_do_Ceará.svg",
        "BR-DF" to "Bandeira_do_Distrito_Federal_(Brasil).svg",
        "BR-ES" to "Bandeira_do_Espírito_Santo.svg",
        "BR-GO" to "Flag_of_Goiás.svg",
        "BR-MA" to "Bandeira_do_Maranhão.svg",
        "BR-MT" to "Bandeira_de_Mato_Grosso.svg",
        "BR-MS" to "Bandeira_de_Mato_Grosso_do_Sul.svg",
        "BR-MG" to "Bandeira_de_Minas_Gerais.svg",
        "BR-PA" to "Bandeira_do_Pará.svg",
        "BR-PB" to "Bandeira_da_Paraíba.svg",
        "BR-PR" to "Bandeira_do_Paraná.svg",
        "BR-PE" to "Bandeira_de_Pernambuco.svg",
        "BR-PI" to "Bandeira_do_Piauí.svg",
        "BR-RJ" to "Bandeira_do_estado_do_Rio_de_Janeiro.svg",
        "BR-RN" to "Bandeira_do_Rio_Grande_do_Norte.svg",
        "BR-RS" to "Bandeira_do_Rio_Grande_do_Sul.svg",
        "BR-RO" to "Bandeira_de_Rondônia.svg",
        "BR-RR" to "Bandeira_de_Roraima.svg",
        "BR-SC" to "Bandeira_de_Santa_Catarina.svg",
        "BR-SP" to "Bandeira_do_estado_de_São_Paulo.svg",
        "BR-SE" to "Bandeira_de_Sergipe.svg",
        "BR-TO" to "Bandeira_do_Tocantins.svg"
    )

    private val germanyStateFlagFiles = mapOf(
        "DE-BW" to "Flag_of_Baden-Württemberg.svg",
        "DE-BY" to "Flag_of_Bavaria_(lozengy).svg",
        "DE-BE" to "Flag_of_Berlin.svg",
        "DE-BB" to "Flag_of_Brandenburg.svg",
        "DE-HB" to "Flag_of_Bremen.svg",
        "DE-HH" to "Flag_of_Hamburg.svg",
        "DE-HE" to "Flag_of_Hesse.svg",
        "DE-MV" to "Flag_of_Mecklenburg-Western_Pomerania.svg",
        "DE-NI" to "Flag_of_Lower_Saxony.svg",
        "DE-NW" to "Flag_of_North_Rhine-Westphalia.svg",
        "DE-RP" to "Flag_of_Rhineland-Palatinate.svg",
        "DE-SL" to "Flag_of_Saarland.svg",
        "DE-SN" to "Flag_of_Saxony.svg",
        "DE-ST" to "Flag_of_Saxony-Anhalt.svg",
        "DE-SH" to "Flag_of_Schleswig-Holstein.svg",
        "DE-TH" to "Flag_of_Thuringia.svg"
    )

    override fun getFlag(geoLocation: IGeoLocationModel, parentName: String?): GeoLocationFlagResult? {
        val name = geoLocation.name
        val code = (geoLocation.alias ?: geoLocation.friendlyId).trim().uppercase(Locale.ROOT)
        val type = geoLocation.type

        // 1. Wikidata: official flag (P41) for any country or ISO 3166-2 subdivision.
        val info = wikidata.resolve(geoLocation)
        info?.flagSvgUrl?.let { url ->
            download(name, code, type, url)?.let { return it }
        }

        // 2. Static tables (BR/DE states), used when Wikidata is unreachable or has no flag.
        val staticFile = staticFlagFile(code)
        if (staticFile != null && staticFile != info?.flagFileName?.replace(' ', '_')) {
            download(name, code, type, WikidataGeoResolver.commonsFileUrl(staticFile))?.let { return it }
        }

        // 3. Flag-like SVG thumbnail of the Wikipedia article (exact titles from Wikidata first).
        val titles = listOfNotNull(info?.enWikipediaTitle, name, parentName?.let { "$name ($it)" }).distinct()
        for (title in titles) {
            val svgUrl = findSvgFromWikipediaSummary(title) ?: continue
            download(name, code, type, svgUrl)?.let { return it }
        }
        return null
    }

    private fun staticFlagFile(code: String): String? {
        val m = Regex("^(BR|DE)[.\\-]([A-Z]{2})$").matchEntire(code) ?: return null
        val key = "${m.groupValues[1]}-${m.groupValues[2]}"
        return brazilStateFlagFiles[key] ?: germanyStateFlagFiles[key]
    }

    private fun download(name: String, code: String, type: GeoLocationType, svgUrl: String): GeoLocationFlagResult? {
        val bytes = http.getBytes(svgUrl) ?: return null
        val content = String(bytes, StandardCharsets.UTF_8)
        if (!content.contains("<svg", ignoreCase = true)) {
            logger.warn("WikimediaCommonsFlagProvider: payload from $svgUrl is not an SVG")
            return null
        }
        logger.info("WikimediaCommonsFlagProvider: Retrieved SVG for '$name' (${bytes.size} bytes) from $svgUrl")
        return GeoLocationFlagResult(
            flagName = "Bandeira Oficial de $name",
            flagDescription = "Bandeira oficial de $name ($type - $code).",
            flagImageContent = content,
            rawBytes = bytes,
            isSvg = true,
            mimeType = "image/svg+xml",
            sourceUrl = svgUrl
        )
    }

    private fun findSvgFromWikipediaSummary(title: String): String? {
        val encoded = URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8)
        val json = http.getString("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded") ?: return null
        val thumbSrc = runCatching { objectMapper.readTree(json).path("thumbnail").path("source").asText("") }.getOrDefault("")
        if (!thumbSrc.contains(".svg", ignoreCase = true)) return null
        // ".../commons/thumb/5/5c/Flag.svg/500px-Flag.svg.png" -> ".../commons/5/5c/Flag.svg"
        val match = Regex(""".*commons/thumb/([0-9a-f]/[0-9a-f]{2}/[^/]+\.svg)/.*""", RegexOption.IGNORE_CASE).find(thumbSrc)
            ?: return null
        // Article thumbnails are often location maps or coats of arms, not flags.
        val file = match.groupValues[1]
        if (!Regex("flag|bandeira|flagge|drapeau|bandera", RegexOption.IGNORE_CASE).containsMatchIn(file)) return null
        return "https://upload.wikimedia.org/wikipedia/commons/$file"
    }
}
