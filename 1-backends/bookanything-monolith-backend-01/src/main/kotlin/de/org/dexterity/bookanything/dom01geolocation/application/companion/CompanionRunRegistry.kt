package de.org.dexterity.bookanything.dom01geolocation.application.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import java.util.Collections

/**
 * Remembers which items each Companion-started import was asked for, so the progress page can draw its
 * full table from the first second (the workflow's progress does not list its items). Bounded and in
 * memory: after a restart the stream still works, with generic row labels.
 */
class CompanionRunRegistry(private val capacity: Int = 100) {

    private val runs: MutableMap<String, List<CountryImportItem>> = Collections.synchronizedMap(
        object : LinkedHashMap<String, List<CountryImportItem>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<CountryImportItem>>?) = size > capacity
        }
    )

    fun remember(workflowId: String, items: List<CountryImportItem>) { runs[workflowId] = items }

    fun itemsOf(workflowId: String): List<CountryImportItem>? = runs[workflowId]
}
