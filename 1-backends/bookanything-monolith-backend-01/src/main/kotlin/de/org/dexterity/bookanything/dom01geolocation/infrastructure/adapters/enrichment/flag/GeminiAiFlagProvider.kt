package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationFlagResult
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationFlagProvider
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
@Order(50)
class GeminiAiFlagProvider(
    private val searchEngineInIAProxyPort: SearchEngineInIAProxyPort,
    private val objectMapper: ObjectMapper
) : IGeoLocationFlagProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "GEMINI_IA"
    override val order: Int = 50

    override fun getFlag(geoLocation: IGeoLocationModel, parentName: String?): GeoLocationFlagResult? {
        val name = geoLocation.name
        val type = geoLocation.type
        val code = geoLocation.alias ?: geoLocation.friendlyId
        val parent = parentName ?: "Global / Continente"

        val prompt = """
            Atue como um especialista em vexilologia (estudo de bandeiras) e geografia política.
            Para a localidade abaixo, identifique a bandeira oficial:
            - Nome: $name
            - Tipo de Localidade: ${type.name}
            - Região / Pertencimento: $parent
            - Código / Alias: $code

            Responda ESTRITAMENTE em formato JSON válido (sem texto fora do bloco json):
            {
              "flagName": "Nome oficial da bandeira",
              "flagDescription": "Descrição heráldica e simbólica concisa das cores e elementos da bandeira (máx 200 caracteres)",
              "svgMarkup": "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 900 600\">...</svg>"
            }
        """.trimIndent()

        return try {
            logger.info("GeminiAiFlagProvider: Requesting AI vexillological synthesis for '$name' ($type)...")
            val rawAiResponse = searchEngineInIAProxyPort.simpleSearchByPrompt(prompt)

            if (!rawAiResponse.isNullOrBlank() && !rawAiResponse.contains("not configured in this environment", ignoreCase = true)) {
                val cleanJson = extractJson(rawAiResponse)
                val rootNode = objectMapper.readTree(cleanJson)
                val flagName = rootNode.path("flagName").asText("Bandeira de $name")
                val flagDescription = rootNode.path("flagDescription").asText("Bandeira oficial de $name.")
                val svgMarkup = rootNode.path("svgMarkup").asText("")

                if (svgMarkup.contains("<svg", ignoreCase = true) && svgMarkup.contains("</svg>", ignoreCase = true)) {
                    val bytes = svgMarkup.toByteArray(StandardCharsets.UTF_8)
                    logger.info("GeminiAiFlagProvider: Generated AI vector SVG for '$name' (${bytes.size} bytes)")
                    GeoLocationFlagResult(
                        flagName = flagName,
                        flagDescription = flagDescription,
                        flagImageContent = svgMarkup,
                        rawBytes = bytes,
                        isSvg = true,
                        mimeType = "image/svg+xml",
                        sourceUrl = "ai://gemini/vector-flag"
                    )
                } else null
            } else null
        } catch (e: Exception) {
            val rootCause = NestedExceptionUtils.getMostSpecificCause(e)
            logger.warn("GeminiAiFlagProvider: AI flag generation failed for '$name': [${rootCause.javaClass.simpleName}] ${rootCause.message}")
            null
        }
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }
}
