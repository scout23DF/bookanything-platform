package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.wikidata

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.http.ResilientHttpFetcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/** What Wikidata knows about a GeoLocation, looked up by its ISO code. */
data class WikidataGeoInfo(
    val qid: String,
    val flagFileName: String?,
    val ptWikipediaTitle: String?,
    val enWikipediaTitle: String?
) {
    val flagSvgUrl: String? get() = flagFileName?.let { WikidataGeoResolver.commonsFileUrl(it) }
}

/**
 * Resolves countries (ISO 3166-1, P298/P297) and first-level subdivisions (ISO 3166-2, P300)
 * to their Wikidata item, official flag file (P41) and pt/en Wikipedia article titles.
 *
 * GADM already gives us these codes as the GeoLocation alias ("DEU", "DE.BW", "BR.SP"),
 * so this replaces hand-maintained per-country tables: the previous hardcoded Wikimedia
 * URLs had wrong hash directories for 12 of 43 flags plus one renamed file (404s) and
 * only covered BR and DE,
 * and Wikipedia titles needed per-state disambiguation lists ("Bremen (estado)").
 *
 * Two Wikidata API calls per new code (~1s), then cached for the app's lifetime,
 * including negative results.
 */
@Component
class WikidataGeoResolver(
    private val http: ResilientHttpFetcher,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val cache = ConcurrentHashMap<String, Optional<WikidataGeoInfo>>()

    fun resolve(geoLocation: IGeoLocationModel): WikidataGeoInfo? {
        val (property, code) = isoStatementFor(geoLocation) ?: return null
        val key = "$property=$code"
        cache[key]?.let { return it.orElse(null) }
        return try {
            lookup(property, code).also { cache[key] = Optional.ofNullable(it) }
        } catch (e: Exception) {
            // Transient (HTTP error, timeout, bad payload): not cached, retried on next call.
            logger.warn("Wikidata lookup failed for $key: [${e.javaClass.simpleName}] ${e.message}")
            null
        }
    }

    /** Returns null when Wikidata has no such item; throws when Wikidata couldn't be reached. */
    private fun lookup(property: String, code: String): WikidataGeoInfo? {
        val search = http.getString(api(mapOf(
            "action" to "query", "list" to "search", "srlimit" to "1",
            "srsearch" to "haswbstatement:$property=$code"
        ))) ?: throw IllegalStateException("search request failed")
        val qid = objectMapper.readTree(search).path("query").path("search").path(0).path("title").asText("")
        if (!qid.matches(Regex("Q\\d+"))) {
            logger.info("Wikidata: no item with $property=$code")
            return null
        }

        val entityJson = http.getString(api(mapOf(
            "action" to "wbgetentities", "ids" to qid,
            "props" to "claims|sitelinks", "sitefilter" to "ptwiki|enwiki"
        ))) ?: throw IllegalStateException("wbgetentities request failed")
        val entity = objectMapper.readTree(entityJson).path("entities").path(qid)

        return WikidataGeoInfo(
            qid = qid,
            flagFileName = currentFlagFile(entity.path("claims").path("P41")),
            ptWikipediaTitle = entity.path("sitelinks").path("ptwiki").path("title").asText(null),
            enWikipediaTitle = entity.path("sitelinks").path("enwiki").path("title").asText(null)
        ).also { logger.info("Wikidata: $property=$code -> $it") }
    }

    /**
     * P41 often lists historical flags too. Prefer the preferred-rank claim, then a normal-rank
     * claim without an end time (P582), then whatever comes first.
     */
    private fun currentFlagFile(claims: JsonNode): String? {
        if (!claims.isArray || claims.isEmpty) return null
        val all = claims.toList().filter { it.path("rank").asText() != "deprecated" }
        val chosen = all.firstOrNull { it.path("rank").asText() == "preferred" }
            ?: all.firstOrNull { !it.path("qualifiers").has("P582") }
            ?: all.firstOrNull()
        return chosen?.path("mainsnak")?.path("datavalue")?.path("value")?.asText(null)
    }

    private fun api(params: Map<String, String>): String =
        "https://www.wikidata.org/w/api.php?format=json&" + params.entries.joinToString("&") { (k, v) ->
            "$k=" + URLEncoder.encode(v, StandardCharsets.UTF_8)
        }

    companion object {
        private val SUBDIVISION = Regex("^([A-Z]{2})[.\\-]([A-Z0-9]{1,3})$")

        /** GADM alias -> Wikidata ISO statement: "DEU" -> P298, "DE" -> P297, "DE.BW"/"DE-BW" -> P300 "DE-BW". */
        fun isoStatementFor(geoLocation: IGeoLocationModel): Pair<String, String>? {
            val code = (geoLocation.alias ?: geoLocation.friendlyId).trim().uppercase(Locale.ROOT)
            return when (geoLocation.type) {
                GeoLocationType.COUNTRY -> when {
                    code.length == 3 && code.all { it.isLetter() } -> "P298" to code
                    code.length == 2 && code.all { it.isLetter() } -> "P297" to code
                    else -> null
                }
                GeoLocationType.PROVINCE -> SUBDIVISION.matchEntire(code)?.let { m ->
                    "P300" to "${m.groupValues[1]}-${m.groupValues[2]}"
                }
                else -> null
            }
        }

        /**
         * Commons original-file URL. The two directory levels are the first 1 and 2 hex chars
         * of MD5(file name with spaces as underscores), so they can be computed instead of
         * being copied by hand.
         */
        fun commonsFileUrl(fileName: String): String {
            val name = fileName.trim().replace(' ', '_')
            val md5 = MessageDigest.getInstance("MD5").digest(name.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20")
            return "https://upload.wikimedia.org/wikipedia/commons/${md5[0]}/${md5.substring(0, 2)}/$encoded"
        }
    }
}
