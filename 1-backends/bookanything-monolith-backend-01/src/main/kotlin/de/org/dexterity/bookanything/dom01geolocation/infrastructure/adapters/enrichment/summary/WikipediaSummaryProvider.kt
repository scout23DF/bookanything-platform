package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
@Order(10)
class WikipediaSummaryProvider(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper
) : IGeoLocationSummaryProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "WIKIPEDIA"
    override val order: Int = 10

    override fun getSummary(geoLocation: IGeoLocationModel, parentName: String?): String? {
        val name = geoLocation.name.trim()
        val type = geoLocation.type.name
        val parent = parentName?.trim()

        val candidateTitles = buildCandidateTitles(name, parent)

        for (title in candidateTitles) {
            val encodedTitle = URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8)
            
            // 1. Try Portuguese Wikipedia first
            val ptSummary = fetchWikipediaExtract("pt", encodedTitle)
            if (!ptSummary.isNullOrBlank()) {
                logger.info("WikipediaSummaryProvider: Found PT extract for '$title' (${ptSummary.length} chars)")
                return ptSummary.take(2000)
            }

            // 2. Try English Wikipedia
            val enSummary = fetchWikipediaExtract("en", encodedTitle)
            if (!enSummary.isNullOrBlank()) {
                logger.info("WikipediaSummaryProvider: Found EN extract for '$title' (${enSummary.length} chars)")
                return enSummary.take(2000)
            }
        }

        logger.debug("WikipediaSummaryProvider: No extract found across candidates $candidateTitles")
        return null
    }

    private fun buildCandidateTitles(name: String, parent: String?): List<String> {
        val titles = mutableListOf<String>()
        titles.add(name)

        // German state title normalization if English/German differs
        when (name.lowercase()) {
            "bavaria", "bayern" -> { titles.add("Baviera"); titles.add("Bavaria") }
            "hesse", "hessen" -> { titles.add("Hesse"); titles.add("Hessen") }
            "saxony", "sachsen" -> { titles.add("Saxônia"); titles.add("Saxony") }
            "lower saxony", "niedersachsen" -> { titles.add("Baixa Saxônia"); titles.add("Lower Saxony") }
            "north rhine-westphalia", "nordrhein-westfalen" -> { titles.add("Renânia do Norte-Vestfália"); titles.add("North Rhine-Westphalia") }
            "rhineland-palatinate", "rheinland-pfalz" -> { titles.add("Renânia-Palatinado"); titles.add("Rhineland-Palatinate") }
            "thuringia", "thüringen" -> { titles.add("Turíngia"); titles.add("Thuringia") }
        }

        // Parent disambiguation (e.g. "Acre (estado)", "Amazonas (Brasil)")
        if (!parent.isNullOrBlank()) {
            titles.add("$name ($parent)")
        }

        return titles.distinct()
    }

    private fun fetchWikipediaExtract(lang: String, encodedTitle: String): String? {
        val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"
        return try {
            val json = webClient.get()
                .uri(url)
                .header("User-Agent", "BookAnythingApp/1.0 (dev@darueira.org)")
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofMillis(3500))
                .block()

            if (json != null) {
                val node: JsonNode = objectMapper.readTree(json)
                val type = node.path("type").asText()
                if (type == "disambiguation") {
                    return null
                }
                val extract = node.path("extract").asText(null)
                if (!extract.isNullOrBlank()) extract else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
