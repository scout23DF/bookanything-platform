package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.time.Duration
import java.util.Locale

@Component
@Order(20)
class RestCountriesSummaryProvider(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper
) : IGeoLocationSummaryProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "REST_COUNTRIES"
    override val order: Int = 20

    override fun supports(geoLocation: IGeoLocationModel): Boolean {
        return geoLocation.type == GeoLocationType.COUNTRY
    }

    override fun getSummary(geoLocation: IGeoLocationModel, parentName: String?): String? {
        val code = geoLocation.alias ?: geoLocation.friendlyId
        val cleanCode = code.trim().lowercase(Locale.ROOT)
        if (cleanCode.length !in 2..3 || !cleanCode.all { it.isLetter() }) {
            return null
        }

        val url = "https://restcountries.com/v3.1/alpha/$cleanCode"
        return try {
            val uri = URI.create(url)
            val json = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofMillis(3500))
                .block() ?: return null

            val root: JsonNode = objectMapper.readTree(json)
            val countryNode = if (root.isArray && root.size() > 0) root[0] else root

            val commonName = countryNode.path("name").path("common").asText(geoLocation.name)
            val officialName = countryNode.path("name").path("official").asText(commonName)
            val capital = countryNode.path("capital").path(0).asText("N/A")
            val region = countryNode.path("region").asText("N/A")
            val subregion = countryNode.path("subregion").asText("N/A")
            val population = countryNode.path("population").asLong(0)
            val area = countryNode.path("area").asDouble(0.0)

            val languagesNode = countryNode.path("languages")
            val languages = if (languagesNode.isObject) {
                languagesNode.elements().asSequence().map { it.asText() }.joinToString(", ")
            } else "N/A"

            val currenciesNode = countryNode.path("currencies")
            val currencies = if (currenciesNode.isObject) {
                currenciesNode.fields().asSequence().map { (currCode, currObj) ->
                    "${currObj.path("name").asText(currCode)} (${currObj.path("symbol").asText(currCode)})"
                }.joinToString(", ")
            } else "N/A"

            val summary = """
                1. Contexto Geopolítico e Histórico:
                $officialName é um Estado soberano localizado na região de $region ($subregion). Possui capital administrativa em $capital e código internacional de referência $code.
                
                2. Perfil Populacional e Demográfico:
                Sua população registrada é de aproximadamente ${String.format(Locale.US, "%,d", population)} habitantes, distribuídos em uma área territorial de ${String.format(Locale.US, "%,.1f", area)} km². Idiomas oficiais reconhecidos: $languages.
                
                3. Economia e Moeda:
                Moeda e sistema financeiro oficial: $currencies. Integra importantes acordos multilaterais e rotas comerciais em seu continente.
            """.trimIndent()

            logger.info("RestCountriesSummaryProvider: Successfully compiled demographic summary for '$commonName'")
            summary.take(2000)
        } catch (e: Exception) {
            logger.debug("RestCountriesSummaryProvider: Error fetching country data for '$code': ${e.message}")
            null
        }
    }
}
