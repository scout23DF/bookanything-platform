package de.org.dexterity.bookanything.dom01geolocation.domain.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem

/** A country as the interpreter understood it, before validation: free text (or ISO3) plus the levels it heard. */
data class CountryMention(val country: String, val levels: List<Int>)

/** What an interpreter heard. It is untrusted input: nothing here is allowed to reach the import before validation. */
data class RawIntent(val mentions: List<CountryMention>)

enum class InterpretationSource { GEMINI, RULES, NONE }

/** The validated, typed answer to "what does the user want imported?". Only [items] may start a workflow. */
data class InterpretationResult(
    val items: List<CountryImportItem>,
    val source: InterpretationSource,
    val warnings: List<String> = emptyList()
) {
    val summary: String
        get() = items.groupBy { it.countrySlug }
            .entries.joinToString("; ") { (slug, list) -> "$slug (level ${list.map { it.locationLevel }.sorted().joinToString(", ")})" }
}

/** Interprets a transcript (typed or dictated) into a [RawIntent]; `null` means "could not". */
interface VoiceCommandInterpreterPort {
    suspend fun interpret(transcript: String): RawIntent?
}
