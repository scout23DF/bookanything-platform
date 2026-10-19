package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryMention
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.RawIntent
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.VoiceCommandInterpreterPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import org.slf4j.LoggerFactory

/**
 * Asks Gemini to turn a free-form sentence (any language) into a small JSON document, and reads it back.
 *
 * The model only *proposes*: this class returns a [RawIntent] and nothing it says is trusted until the
 * validator accepts it. Reading is done on the JSON tree (no reflection on DTOs), which also keeps the
 * GraalVM native image simple. Any malformed answer is `null`, which makes the caller use the rules.
 */
class GeminiVoiceCommandInterpreter(
    private val gemini: SearchEngineInIAProxyPort,
    private val objectMapper: ObjectMapper
) : VoiceCommandInterpreterPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun interpret(transcript: String): RawIntent? {
        val answer = gemini.simpleSearchByPromptSuspend(prompt(transcript)) ?: return null
        return parse(answer)
    }

    internal fun parse(answer: String): RawIntent? {
        val start = answer.indexOf('{')
        val end = answer.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val root = objectMapper.readTree(answer.substring(start, end + 1))
            val items = root.path("items")
            if (!items.isArray) return null
            RawIntent(
                items.mapNotNull { node ->
                    val country = node.path("country").asText("").ifBlank { return@mapNotNull null }
                    CountryMention(country, node.path("levels").filter { it.canConvertToInt() }.map { it.asInt() })
                },
                root.path("mapSvgGeneratorMechanism").asText("").ifBlank { null }
            )
        } catch (e: Exception) {
            logger.warn("Companion: could not read Gemini's answer as JSON: ${e.message}")
            null
        }
    }

    // TOFDO: Explain this later
    private fun prompt(transcript: String): String = """
        You extract data-import commands. The user dictated or typed this sentence (any language):
        "${transcript.replace("\"", "'")}"
        Answer with ONLY a JSON object, no markdown, in exactly this shape:
        {"items":[{"country":"<country name in English>","levels":[<integers>]}],"mapSvgGeneratorMechanism":<null or "KOTLIN" or "GEOPANDAS">}
        "levels" are administrative levels from 0 (country) to 3. If the user gave none, use [0].
        "Up to level N" (also "até o nível N", "bis Stufe N", "hasta el nivel N") means every level from 0 to N,
        e.g. up to level 2 is [0,1,2]; "level N" or "only level N" alone is just [N].
        "mapSvgGeneratorMechanism" is how the map images are drawn: "GEOPANDAS" if the user asks for GeoPandas (or Python/pandas maps),
        "KOTLIN" if the user asks for Kotlin, and null if the user says nothing about it. Never guess it.
        If the sentence names no country, answer {"items":[],"mapSvgGeneratorMechanism":null}.
    """.trimIndent()
}
