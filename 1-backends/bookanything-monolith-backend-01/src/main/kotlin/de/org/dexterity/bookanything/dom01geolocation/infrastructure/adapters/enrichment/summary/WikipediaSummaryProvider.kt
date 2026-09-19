package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
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

        // Specific disambiguation for Brazilian states where base name might be a city or ambiguous
        when (name.trim().lowercase()) {
            "distrito federal" -> titles.add("Distrito Federal (Brasil)")
            "espírito santo" -> titles.add("Espírito Santo (estado)")
            "rio de janeiro" -> titles.add("Rio de Janeiro (estado)")
            "são paulo" -> titles.add("São Paulo (estado)")
        }

        // German state title normalization if English/German differs
        when (name.trim().lowercase()) {
            "bavaria", "bayern" -> { titles.add("Baviera"); titles.add("Bavaria"); titles.add("Bayern") }
            "hesse", "hessen" -> { titles.add("Hesse"); titles.add("Hessen") }
            "saxony", "sachsen" -> { titles.add("Saxônia"); titles.add("Saxony"); titles.add("Sachsen") }
            "lower saxony", "niedersachsen" -> { titles.add("Baixa Saxônia"); titles.add("Lower Saxony"); titles.add("Niedersachsen") }
            "north rhine-westphalia", "nordrhein-westfalen" -> { titles.add("Renânia do Norte-Vestfália"); titles.add("North Rhine-Westphalia"); titles.add("Nordrhein-Westfalen") }
            "rhineland-palatinate", "rheinland-pfalz" -> { titles.add("Renânia-Palatinado"); titles.add("Rhineland-Palatinate"); titles.add("Rheinland-Pfalz") }
            "thuringia", "thüringen" -> { titles.add("Turíngia"); titles.add("Thuringia"); titles.add("Thüringen") }
            "bremen" -> { titles.add("Bremen (estado)"); titles.add("Freie Hansestadt Bremen"); titles.add("Bremen") }
            "hamburg" -> { titles.add("Hamburgo"); titles.add("Hamburg") }
            "berlin" -> { titles.add("Berlim"); titles.add("Berlin") }
            "brandenburg" -> { titles.add("Brandemburgo"); titles.add("Brandenburg") }
            "mecklenburg-vorpommern" -> { titles.add("Meclemburgo-Pomerânia Ocidental"); titles.add("Mecklenburg-Western Pomerania"); titles.add("Mecklenburg-Vorpommern") }
            "saarland" -> { titles.add("Sarre"); titles.add("Saarland") }
            "saxony-anhalt", "sachsen-anhalt" -> { titles.add("Saxônia-Anhalt"); titles.add("Saxony-Anhalt"); titles.add("Sachsen-Anhalt") }
            "schleswig-holstein" -> { titles.add("Eslésvico-Holsácia"); titles.add("Schleswig-Holstein") }
        }

        titles.add(name)

        // Additional state-level disambiguations
        titles.add("$name (estado)")
        if (!parent.isNullOrBlank()) {
            titles.add("$name ($parent)")
        }

        return titles.distinct()
    }

    private fun fetchWikipediaExtract(lang: String, encodedTitle: String): String? {
        val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"
        return try {
            val uri = URI.create(url)
            val json = webClient.get()
                .uri(uri)
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
