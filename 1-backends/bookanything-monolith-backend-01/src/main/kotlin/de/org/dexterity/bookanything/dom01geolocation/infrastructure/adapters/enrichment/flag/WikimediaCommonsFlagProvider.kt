package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationFlagResult
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Locale

@Component
@Order(20)
class WikimediaCommonsFlagProvider(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper
) : IGeoLocationFlagProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "WIKIMEDIA"
    override val order: Int = 20

    // Direct mapping for Brazilian states (ISO 3166-2 / GADM) to Wikimedia Commons canonical SVGs
    private val brazilStateSvgUrls = mapOf(
        "BR-AC" to "https://upload.wikimedia.org/wikipedia/commons/4/4c/Bandeira_do_Acre.svg",
        "BR-AL" to "https://upload.wikimedia.org/wikipedia/commons/8/88/Bandeira_de_Alagoas.svg",
        "BR-AP" to "https://upload.wikimedia.org/wikipedia/commons/0/0c/Bandeira_do_Amap%C3%A1.svg",
        "BR-AM" to "https://upload.wikimedia.org/wikipedia/commons/6/6b/Bandeira_do_Amazonas.svg",
        "BR-BA" to "https://upload.wikimedia.org/wikipedia/commons/2/28/Bandeira_da_Bahia.svg",
        "BR-CE" to "https://upload.wikimedia.org/wikipedia/commons/2/2e/Bandeira_do_Cear%C3%A1.svg",
        "BR-DF" to "https://upload.wikimedia.org/wikipedia/commons/3/3c/Bandeira_do_Distrito_Federal_%28Brasil%29.svg",
        "BR-ES" to "https://upload.wikimedia.org/wikipedia/commons/4/43/Bandeira_do_Esp%C3%ADrito_Santo.svg",
        "BR-GO" to "https://upload.wikimedia.org/wikipedia/commons/b/be/Bandeira_de_Goi%C3%A1s.svg",
        "BR-MA" to "https://upload.wikimedia.org/wikipedia/commons/4/45/Bandeira_do_Maranh%C3%A3o.svg",
        "BR-MT" to "https://upload.wikimedia.org/wikipedia/commons/0/0b/Bandeira_de_Mato_Grosso.svg",
        "BR-MS" to "https://upload.wikimedia.org/wikipedia/commons/6/64/Bandeira_de_Mato_Grosso_do_Sul.svg",
        "BR-MG" to "https://upload.wikimedia.org/wikipedia/commons/f/f4/Bandeira_de_Minas_Gerais.svg",
        "BR-PA" to "https://upload.wikimedia.org/wikipedia/commons/0/02/Bandeira_do_Par%C3%A1.svg",
        "BR-PB" to "https://upload.wikimedia.org/wikipedia/commons/b/bb/Bandeira_da_Para%C3%ADba.svg",
        "BR-PR" to "https://upload.wikimedia.org/wikipedia/commons/9/93/Bandeira_do_Paran%C3%A1.svg",
        "BR-PE" to "https://upload.wikimedia.org/wikipedia/commons/5/59/Bandeira_de_Pernambuco.svg",
        "BR-PI" to "https://upload.wikimedia.org/wikipedia/commons/3/33/Bandeira_do_Piau%C3%AD.svg",
        "BR-RJ" to "https://upload.wikimedia.org/wikipedia/commons/7/73/Bandeira_do_estado_do_Rio_de_Janeiro.svg",
        "BR-RN" to "https://upload.wikimedia.org/wikipedia/commons/3/30/Bandeira_do_Rio_Grande_do_Norte.svg",
        "BR-RS" to "https://upload.wikimedia.org/wikipedia/commons/6/63/Bandeira_do_Rio_Grande_do_Sul.svg",
        "BR-RO" to "https://upload.wikimedia.org/wikipedia/commons/f/fa/Bandeira_de_Rond%C3%B4nia.svg",
        "BR-RR" to "https://upload.wikimedia.org/wikipedia/commons/9/98/Bandeira_de_Roraima.svg",
        "BR-SC" to "https://upload.wikimedia.org/wikipedia/commons/1/1a/Bandeira_de_Santa_Catarina.svg",
        "BR-SP" to "https://upload.wikimedia.org/wikipedia/commons/2/2b/Bandeira_do_estado_de_S%C3%A3o_Paulo.svg",
        "BR-SE" to "https://upload.wikimedia.org/wikipedia/commons/9/9b/Bandeira_de_Sergipe.svg",
        "BR-TO" to "https://upload.wikimedia.org/wikipedia/commons/f/ff/Bandeira_do_Tocantins.svg"
    )

    // Direct mapping for German Federal States to Wikimedia Commons canonical SVGs
    private val germanyStateSvgUrls = mapOf(
        "DE-BW" to "https://upload.wikimedia.org/wikipedia/commons/5/5c/Flag_of_Baden-W%C3%BCrttemberg.svg",
        "DE-BY" to "https://upload.wikimedia.org/wikipedia/commons/2/20/Flag_of_Bavaria_%28lozengy%29.svg",
        "DE-BE" to "https://upload.wikimedia.org/wikipedia/commons/e/ec/Flag_of_Berlin.svg",
        "DE-BB" to "https://upload.wikimedia.org/wikipedia/commons/d/d4/Flag_of_Brandenburg.svg",
        "DE-HB" to "https://upload.wikimedia.org/wikipedia/commons/0/07/Flag_of_Bremen.svg",
        "DE-HH" to "https://upload.wikimedia.org/wikipedia/commons/7/74/Flag_of_Hamburg.svg",
        "DE-HE" to "https://upload.wikimedia.org/wikipedia/commons/a/ad/Flag_of_Hesse.svg",
        "DE-MV" to "https://upload.wikimedia.org/wikipedia/commons/1/10/Flag_of_Mecklenburg-Western_Pomerania.svg",
        "DE-NI" to "https://upload.wikimedia.org/wikipedia/commons/7/74/Flag_of_Lower_Saxony.svg",
        "DE-NW" to "https://upload.wikimedia.org/wikipedia/commons/c/c1/Flag_of_North_Rhine-Westphalia.svg",
        "DE-RP" to "https://upload.wikimedia.org/wikipedia/commons/b/b6/Flag_of_Rhineland-Palatinate.svg",
        "DE-SL" to "https://upload.wikimedia.org/wikipedia/commons/f/f7/Flag_of_Saarland.svg",
        "DE-SN" to "https://upload.wikimedia.org/wikipedia/commons/e/e9/Flag_of_Saxony.svg",
        "DE-ST" to "https://upload.wikimedia.org/wikipedia/commons/b/b8/Flag_of_Saxony-Anhalt.svg",
        "DE-SH" to "https://upload.wikimedia.org/wikipedia/commons/2/2b/Flag_of_Schleswig-Holstein.svg",
        "DE-TH" to "https://upload.wikimedia.org/wikipedia/commons/8/87/Flag_of_Thuringia.svg"
    )

    override fun getFlag(geoLocation: IGeoLocationModel, parentName: String?): GeoLocationFlagResult? {
        val name = geoLocation.name
        val code = (geoLocation.alias ?: geoLocation.friendlyId).trim().uppercase(Locale.ROOT)
        val type = geoLocation.type

        // 1. Check direct Brazilian state mapping (handles BR-SP, BR.SP, SP)
        val brKey = normalizeSubdivisionKey("BR", code)
        if (brKey != null && brazilStateSvgUrls.containsKey(brKey)) {
            val svgUrl = brazilStateSvgUrls[brKey]!!
            logger.info("WikimediaCommonsFlagProvider: Found direct SVG mapping for Brazilian state '$name' ($brKey): $svgUrl")
            return downloadAndBuildResult(name, code, type, svgUrl)
        }

        // 2. Check direct German state mapping (handles DE-BY, DE.BY, BY)
        val deKey = normalizeSubdivisionKey("DE", code)
        if (deKey != null && germanyStateSvgUrls.containsKey(deKey)) {
            val svgUrl = germanyStateSvgUrls[deKey]!!
            logger.info("WikimediaCommonsFlagProvider: Found direct SVG mapping for German state '$name' ($deKey): $svgUrl")
            return downloadAndBuildResult(name, code, type, svgUrl)
        }

        // 3. Dynamic lookup via Wikipedia page summary thumbnail
        val dynamicSvgUrl = findSvgFromWikipediaSummary(name, parentName)
        if (dynamicSvgUrl != null) {
            logger.info("WikimediaCommonsFlagProvider: Discovered Wikimedia SVG for '$name' via Wikipedia API: $dynamicSvgUrl")
            return downloadAndBuildResult(name, code, type, dynamicSvgUrl)
        }

        return null
    }

    private fun normalizeSubdivisionKey(countryPrefix: String, code: String): String? {
        if (code.startsWith("$countryPrefix-") || code.startsWith("$countryPrefix.")) {
            return "$countryPrefix-" + code.substring(3).take(2)
        }
        if (code.length == 2 && code.all { it.isLetter() }) {
            return "$countryPrefix-$code"
        }
        return null
    }

    private fun downloadAndBuildResult(
        name: String,
        code: String,
        type: GeoLocationType,
        svgUrl: String
    ): GeoLocationFlagResult? {
        val bytes = downloadBytes(svgUrl) ?: return null
        val content = String(bytes, StandardCharsets.UTF_8)
        if (!content.contains("<svg", ignoreCase = true)) {
            logger.warn("WikimediaCommonsFlagProvider: Downloaded payload from $svgUrl is not a valid SVG")
            return null
        }

        logger.info("WikimediaCommonsFlagProvider: Successfully retrieved SVG for '$name' (${bytes.size} bytes)")
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

    private fun findSvgFromWikipediaSummary(name: String, parent: String?): String? {
        val candidates = listOfNotNull(name, if (!parent.isNullOrBlank()) "$name ($parent)" else null)
        for (cand in candidates) {
            val encoded = URLEncoder.encode(cand.replace(" ", "_"), StandardCharsets.UTF_8)
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"
            try {
                val uri = URI.create(url)
                val json = webClient.get()
                    .uri(uri)
                    .header("User-Agent", "BookAnythingApp/1.0 (dev@darueira.org)")
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .timeout(Duration.ofMillis(3000))
                    .block() ?: continue

                val node: JsonNode = objectMapper.readTree(json)
                val thumbSrc = node.path("thumbnail").path("source").asText("")
                if (thumbSrc.contains(".svg", ignoreCase = true)) {
                    // Convert thumbnail URL to original SVG URL:
                    // e.g. "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5c/Flag.svg/500px-Flag.svg.png"
                    // -> "https://upload.wikimedia.org/wikipedia/commons/5/5c/Flag.svg"
                    val regex = """.*commons/thumb/([0-9a-f]/[0-9a-f]{2}/[^/]+\.svg)/.*""".toRegex(RegexOption.IGNORE_CASE)
                    val match = regex.find(thumbSrc)
                    if (match != null) {
                        return "https://upload.wikimedia.org/wikipedia/commons/" + match.groupValues[1]
                    }
                }
            } catch (e: Exception) {
                // ignore and continue
            }
        }
        return null
    }

    private fun downloadBytes(url: String): ByteArray? {
        return try {
            val uri = URI.create(url)
            webClient.get()
                .uri(uri)
                .header("User-Agent", "BookAnythingApp/1.0 (dev@darueira.org)")
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .timeout(Duration.ofMillis(4000))
                .block()
        } catch (e: Exception) {
            logger.debug("WikimediaCommonsFlagProvider: Error downloading $url: ${e.message}")
            null
        }
    }
}
