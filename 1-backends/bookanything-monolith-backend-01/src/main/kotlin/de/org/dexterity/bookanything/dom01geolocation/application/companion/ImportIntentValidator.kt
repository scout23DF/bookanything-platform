package de.org.dexterity.bookanything.dom01geolocation.application.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryCatalog
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryMention
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.InterpretationResult
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.InterpretationSource
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.RawIntent
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem

/**
 * The guardrail between what a language model (or a rule) *heard* and what the system will *do*.
 * The model never calls the API: it proposes a [RawIntent], and only what survives here can become an import.
 */
class ImportIntentValidator(private val maxItems: Int = DEFAULT_MAX_ITEMS) {

    fun validate(raw: RawIntent, source: InterpretationSource): InterpretationResult {
        val warnings = mutableListOf<String>()
        val items = LinkedHashSet<CountryImportItem>()

        for (mention in raw.mentions) {
            val slug = CountryCatalog.resolve(mention.country)
            if (slug == null) {
                warnings += "Unknown country \"${mention.country.take(40)}\" ignored"
                continue
            }
            val levels = mention.levels.ifEmpty { listOf(DEFAULT_LEVEL) }
            for (level in levels) {
                if (level !in MIN_LEVEL..MAX_LEVEL) {
                    warnings += "Level $level for $slug is outside $MIN_LEVEL..$MAX_LEVEL and was ignored"
                    continue
                }
                items += CountryImportItem(countrySlug = slug, locationLevel = level)
            }
        }

        val kept = items.take(maxItems)
        if (items.size > maxItems) warnings += "Only the first $maxItems of ${items.size} requested imports were kept"
        return InterpretationResult(kept, if (kept.isEmpty()) InterpretationSource.NONE else source, warnings)
    }

    /** Re-validates items sent back by the UI on confirmation: the client is not trusted either. */
    fun revalidate(items: List<CountryImportItem>): InterpretationResult =
        validate(
            RawIntent(items.groupBy { it.countrySlug }.map { (slug, list) -> CountryMention(slug, list.map { it.locationLevel }) }),
            InterpretationSource.NONE
        ).let { if (it.items.isEmpty()) it else it.copy(source = InterpretationSource.RULES) }

    companion object {
        const val MIN_LEVEL = 0
        const val MAX_LEVEL = 3
        const val DEFAULT_LEVEL = 0
        const val DEFAULT_MAX_ITEMS = 6
    }
}
