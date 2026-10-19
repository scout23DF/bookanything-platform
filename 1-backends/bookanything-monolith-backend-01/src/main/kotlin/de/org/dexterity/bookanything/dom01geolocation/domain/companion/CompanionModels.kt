package de.org.dexterity.bookanything.dom01geolocation.domain.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem

/** A country as the interpreter understood it, before validation: free text (or ISO3) plus the levels it heard. */
data class CountryMention(val country: String, val levels: List<Int>)

/**
 * How the map SVGs of an import are drawn. The batch has one mechanism, not one per country.
 * The Companion defaults to [GEOPANDAS]; the user can ask for [KOTLIN] (or repeat GeoPandas) by voice.
 */
enum class MapSvgMechanism {
    KOTLIN, GEOPANDAS;

    companion object {
        val DEFAULT = GEOPANDAS

        /** Only the two known names (any case, surrounding spaces ignored) are accepted: anything else is `null`. */
        fun parse(text: String?): MapSvgMechanism? = entries.firstOrNull { it.name.equals(text?.trim(), ignoreCase = true) }
    }
}

/**
 * What an interpreter heard. It is untrusted input: nothing here is allowed to reach the import before validation.
 * [mapSvgGeneratorMechanism] is the raw text of the mechanism the user asked for, or `null` when they said nothing about it.
 */
data class RawIntent(val mentions: List<CountryMention>, val mapSvgGeneratorMechanism: String? = null)

enum class InterpretationSource { GEMINI, RULES, NONE }

/** The validated, typed answer to "what does the user want imported?". Only [items] may start a workflow. */
data class InterpretationResult(
    val items: List<CountryImportItem>,
    val source: InterpretationSource,
    val warnings: List<String> = emptyList(),
    val mapSvgGeneratorMechanism: MapSvgMechanism = MapSvgMechanism.DEFAULT
) {
    val summary: String
        get() = items.groupBy { it.countrySlug }
            .entries.joinToString("; ") { (slug, list) -> "$slug (level ${list.map { it.locationLevel }.sorted().joinToString(", ")})" }
}

/** Interprets a transcript (typed or dictated) into a [RawIntent]; `null` means "could not". */
interface VoiceCommandInterpreterPort {
    suspend fun interpret(transcript: String): RawIntent?
}
