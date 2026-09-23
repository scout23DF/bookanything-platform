package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.flag

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
    private val searchEngineInIAProxyPort: SearchEngineInIAProxyPort
) : IGeoLocationFlagProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "GEMINI_IA"
    override val order: Int = 50

    override fun getFlag(geoLocation: IGeoLocationModel, parentName: String?): GeoLocationFlagResult? {
        val name = geoLocation.name
        val type = geoLocation.type
        val code = geoLocation.alias ?: geoLocation.friendlyId
        val parent = parentName ?: "Global / Continente"

        // Ask for the SVG document only. Embedding the SVG inside a JSON string made the model
        // escape hundreds of quotes, and it regularly got that wrong (JsonParseException), so
        // valid flags were thrown away. Name and description are built locally instead.
        val prompt = """
            Atue como um especialista em vexilologia (estudo de bandeiras).
            Desenhe a bandeira oficial desta localidade como um documento SVG:
            - Nome: $name
            - Tipo de Localidade: ${type.name}
            - Região / Pertencimento: $parent
            - Código / Alias: $code

            Regras:
            - Responda SOMENTE com o SVG, começando em <svg e terminando em </svg>.
            - Use viewBox="0 0 900 600" e xmlns="http://www.w3.org/2000/svg".
            - Sem <script>, sem imagens externas, sem texto fora do SVG.
        """.trimIndent()

        return try {
            logger.info("GeminiAiFlagProvider: Requesting AI vexillological synthesis for '$name' ($type)...")
            val raw = searchEngineInIAProxyPort.simpleSearchByPrompt(prompt) ?: return null
            val svg = extractSvg(raw)
            if (svg == null) {
                logger.warn("GeminiAiFlagProvider: response for '$name' had no complete <svg>...</svg> block (${raw.length} chars)")
                return null
            }
            val bytes = svg.toByteArray(StandardCharsets.UTF_8)
            logger.info("GeminiAiFlagProvider: Generated AI vector SVG for '$name' (${bytes.size} bytes)")
            GeoLocationFlagResult(
                flagName = "Bandeira de $name",
                flagDescription = "Representação da bandeira de $name gerada por IA (não oficial).",
                flagImageContent = svg,
                rawBytes = bytes,
                isSvg = true,
                mimeType = "image/svg+xml",
                sourceUrl = "ai://gemini/vector-flag"
            )
        } catch (e: Exception) {
            val rootCause = NestedExceptionUtils.getMostSpecificCause(e)
            logger.warn("GeminiAiFlagProvider: AI flag generation failed for '$name': [${rootCause.javaClass.simpleName}] ${rootCause.message}")
            null
        }
    }

    /**
     * Pulls the first complete <svg>...</svg> out of the reply (tolerating markdown fences or
     * prose around it) and strips anything active before it is stored and served as an asset.
     */
    private fun extractSvg(raw: String): String? {
        val start = raw.indexOf("<svg", ignoreCase = true)
        val end = raw.lastIndexOf("</svg>", ignoreCase = true)
        if (start < 0 || end < start) return null
        return raw.substring(start, end + "</svg>".length)
            .replace(Regex("(?is)<script\\b.*?</script>"), "")
            .replace(Regex("(?is)<foreignObject\\b.*?</foreignObject>"), "")
            .replace(Regex("(?i)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*')"), "")
            .replace(Regex("(?i)(href\\s*=\\s*[\"'])\\s*javascript:[^\"']*"), "$1#")
    }
}
