package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryCatalog
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryMention
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.RawIntent
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.VoiceCommandInterpreterPort

/**
 * Deterministic interpreter: country names from the catalog plus the level numbers said after a level word
 * ("level", "nível", "stufe", ...). Needs no network and no quota, so the Companion degrades instead of
 * breaking when the language model is slow, out of quota or its circuit breaker is open.
 *
 * Levels apply to every country in the sentence ("Germany and Luxembourg, levels 0 and 1" = four imports).
 * "up to level 2" / "até o nível 2" / "bis Stufe 2" mean 0..2, and "levels 1 to 3" is a range.
 */
class RuleBasedVoiceCommandInterpreter : VoiceCommandInterpreterPort {

    private val levelWord = "(?:levels?|niveis|nivel|stufen|stufe|ebenen|ebene)"
    private val upTo = Regex("(?:up to|until|through|ate|bis)\\s+(?:o |the |zur |zum )?$levelWord\\s*(\\d)")
    private val rangeOrList = Regex("$levelWord\\s*((?:\\d|\\s|,|&|and|e|und|y|to|a|ate|bis|-)+)")

    override suspend fun interpret(transcript: String): RawIntent? {
        val countries = CountryCatalog.findIn(transcript)
        if (countries.isEmpty()) return null
        val levels = levelsIn(CountryCatalog.normalize(transcript))
        return RawIntent(countries.map { CountryMention(it, levels) })
    }

    internal fun levelsIn(text: String): List<Int> {
        upTo.find(text)?.let { return (0..it.groupValues[1].toInt()).toList() }
        val chunk = rangeOrList.find(text)?.groupValues?.get(1) ?: return emptyList()
        val tokens = Regex("\\d|to|bis|ate|a|-").findAll(chunk).map { it.value }.toList()
        val levels = sortedSetOf<Int>()
        var i = 0
        while (i < tokens.size) {
            val current = tokens[i].toIntOrNull()
            if (current == null) { i++; continue }
            val isRange = i + 2 < tokens.size && tokens[i + 1] in setOf("to", "bis", "ate", "-") && tokens[i + 2].toIntOrNull() != null
            if (isRange) {
                levels += current..tokens[i + 2].toInt()
                i += 3
            } else {
                levels += current
                i++
            }
        }
        return levels.toList()
    }
}
