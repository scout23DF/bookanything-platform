package de.org.dexterity.bookanything.dom01geolocation.domain.companion

import java.text.Normalizer
import java.util.Locale

/**
 * The countries the Companion is willing to import, by name in English, Portuguese, German and Spanish, or by
 * ISO 3166 alpha-3 code (the `countrySlug` the import workflow takes). Anything else is rejected, so a
 * mis-heard word can never become an import.
 *
 * The names are listed here, not taken from `Locale#getDisplayCountry`: that needs locale data that a
 * GraalVM native image does not include unless asked to.
 */
object CountryCatalog {

    private val aliases: Map<String, List<String>> = mapOf(
        "DEU" to listOf("germany", "alemanha", "deutschland", "alemania"),
        "LUX" to listOf("luxembourg", "luxemburgo", "luxemburg"),
        "BRA" to listOf("brazil", "brasil", "brasilien"),
        "PRT" to listOf("portugal"),
        "ESP" to listOf("spain", "espanha", "spanien", "espana"),
        "FRA" to listOf("france", "franca", "frankreich", "francia"),
        "ITA" to listOf("italy", "italia", "italien"),
        "GBR" to listOf("united kingdom", "great britain", "uk", "reino unido", "vereinigtes koenigreich", "england"),
        "USA" to listOf("united states", "estados unidos", "vereinigte staaten", "usa"),
        "ARG" to listOf("argentina", "argentinien"),
        "CHL" to listOf("chile"),
        "COL" to listOf("colombia", "kolumbien"),
        "MEX" to listOf("mexico", "mexiko"),
        "PER" to listOf("peru"),
        "URY" to listOf("uruguay"),
        "NLD" to listOf("netherlands", "holland", "holanda", "paises baixos", "niederlande"),
        "BEL" to listOf("belgium", "belgica", "belgien"),
        "CHE" to listOf("switzerland", "suica", "schweiz", "suiza"),
        "AUT" to listOf("austria", "oesterreich", "osterreich"),
        "POL" to listOf("poland", "polonia", "polen"),
        "CZE" to listOf("czechia", "czech republic", "tchequia", "tschechien"),
        "DNK" to listOf("denmark", "dinamarca", "daenemark", "danemark"),
        "SWE" to listOf("sweden", "suecia", "schweden"),
        "NOR" to listOf("norway", "noruega", "norwegen"),
        "FIN" to listOf("finland", "finlandia", "finnland"),
        "IRL" to listOf("ireland", "irlanda", "irland"),
        "GRC" to listOf("greece", "grecia", "griechenland"),
        "TUR" to listOf("turkey", "turquia", "tuerkei", "turkei"),
        "JPN" to listOf("japan", "japao", "japon"),
        "CHN" to listOf("china"),
        "IND" to listOf("india", "indien"),
        "CAN" to listOf("canada", "kanada"),
        "AUS" to listOf("australia", "australien"),
        "NGA" to listOf("nigeria"),
        "NER" to listOf("niger"),
        "ZAF" to listOf("south africa", "africa do sul", "suedafrika", "sudafrika"),
        "EGY" to listOf("egypt", "egito", "aegypten", "agypten"),
        "MAR" to listOf("morocco", "marrocos", "marokko"),
        "RUS" to listOf("russia", "russland"),
        "UKR" to listOf("ukraine", "ucrania", "ukraina"),
        "HUN" to listOf("hungary", "hungria", "ungarn"),
        "ROU" to listOf("romania", "romenia", "rumanien"),
        "LIE" to listOf("liechtenstein"),
        "MCO" to listOf("monaco"),
        "SVN" to listOf("slovenia", "eslovenia", "slowenien"),
        "HRV" to listOf("croatia", "croacia", "kroatien")
    )

    /** Every valid ISO 3166 alpha-3 code, from the JDK tables (these do not depend on locale data). */
    private val iso3Codes: Set<String> =
        Locale.getISOCountries().map { Locale.of("", it).isO3Country }.toSet()

    private val byAlias: Map<String, String> =
        aliases.flatMap { (iso3, names) -> names.map { it to iso3 } }.toMap()

    /** Aliases longest first, so "south africa" wins over "africa", each with the regex that finds it as whole words. */
    private val aliasMatchers: List<Pair<Regex, String>> =
        byAlias.entries.sortedByDescending { it.key.length }
            .map { (alias, iso3) -> Regex("(?<![a-z])${Regex.escape(alias)}(?![a-z])") to iso3 }

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Resolves one country text (a name or an ISO3 code) to its ISO3 code; `null` when it is not in the catalog. */
    fun resolve(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.length == 3 && trimmed.uppercase() in iso3Codes && trimmed.uppercase() in aliases) return trimmed.uppercase()
        return byAlias[normalize(trimmed)]
    }

    /** Finds the countries named in a sentence, in order of appearance, without repeats. */
    fun findIn(sentence: String): List<String> {
        val text = normalize(sentence)
        val found = mutableListOf<Pair<Int, String>>()
        val taken = BooleanArray(text.length)
        for ((regex, iso3) in aliasMatchers) {
            for (match in regex.findAll(text)) {
                val range = match.range
                if (range.any { taken[it] }) continue
                range.forEach { taken[it] = true }
                found += range.first to iso3
            }
        }
        // ISO3 codes typed in capitals ("DEU"), the way the batch API takes them; lower case is a word, not a code
        Regex("\\b[A-Z]{3}\\b").findAll(sentence).forEach {
            val code = it.value
            if (code in aliases) found += (text.indexOf(code.lowercase()).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE) to code
        }
        return found.sortedBy { it.first }.map { it.second }.distinct()
    }
}
