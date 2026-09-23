package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.summary

import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment.IGeoLocationSummaryProvider
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(50)
class GeminiAiSummaryProvider(
    private val searchEngineInIAProxyPort: SearchEngineInIAProxyPort
) : IGeoLocationSummaryProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val providerId: String = "GEMINI_IA"
    override val order: Int = 50

    override fun getSummary(geoLocation: IGeoLocationModel, parentName: String?): String? {
        val name = geoLocation.name
        val type = geoLocation.type.name
        val code = geoLocation.alias ?: geoLocation.friendlyId
        val parent = parentName ?: "Global / Continente"

        val prompt = """
            Atue como um analista sênior em geopolítica, geografia e demografia.
            Elabore uma síntese executiva factual e estruturada sobre a localidade:
            - Nome: $name
            - Tipo: $type
            - Região / Pertencimento: $parent
            - Código / Alias: $code

            Aborde obrigatoriamente de forma estruturada:
            1. Contexto Geopolítico e Histórico
            2. Economia e Principais Atividades
            3. Turismo e Atrativos Culturais
            4. Perfil Populacional e Demográfico

            Regras estritas:
            - Responda em português, com redação executiva e informativa.
            - O texto TOTAL NÃO PODE ULTRAPASSAR 2000 CARACTERES.
        """.trimIndent()

        return try {
            logger.info("GeminiAiSummaryProvider: Requesting AI synthesis for '$name' ($type)...")
            val aiResponse = searchEngineInIAProxyPort.simpleSearchByPrompt(prompt)

            // Drop markdown code fences the model sometimes wraps plain text in.
            val summary = aiResponse
                ?.replace(Regex("(?m)^\\s*```[a-zA-Z]*\\s*$"), "")
                ?.trim()
            if (!summary.isNullOrBlank()) {
                logger.info("GeminiAiSummaryProvider: AI summary generated successfully for '$name' (${summary.length} chars)")
                summary.take(2000)
            } else {
                null
            }
        } catch (e: Exception) {
            val rootCause = NestedExceptionUtils.getMostSpecificCause(e)
            logger.warn("GeminiAiSummaryProvider: AI call failed for '$name': [${rootCause.javaClass.simpleName}] ${rootCause.message}")
            null
        }
    }
}
