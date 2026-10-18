package de.org.dexterity.bookanything.dom01geolocation.application.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.ItemState
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.ProgressSnapshot
import org.springframework.web.util.HtmlUtils

/** Renders a [ProgressSnapshot] as the HTML fragment the page swaps in (htmx SSE extension, no app JavaScript). */
object CompanionProgressHtml {

    private fun label(state: ItemState) = when (state) {
        ItemState.PENDING -> "Pending"
        ItemState.INGESTING -> "Ingesting via NiFi"
        ItemState.INGESTED -> "Ingested, waiting for artifacts"
        ItemState.GENERATING_ARTIFACTS -> "Generating maps and AI enrichment"
        ItemState.ARTIFACTS_READY -> "Artifacts ready"
        ItemState.COMPLETED -> "Completed"
        ItemState.FAILED -> "Failed"
    }

    private fun esc(text: String) = HtmlUtils.htmlEscape(text)

    fun render(snapshot: ProgressSnapshot): String = buildString {
        append("""<div class="companion-progress">""")
        append("""<progress max="100" value="${snapshot.percent.coerceIn(0, 100)}"></progress> """)
        append("<span>${esc(snapshot.stage)} &middot; ${snapshot.percent}%</span>")
        append("""<table><thead><tr><th>Country</th><th>Level</th><th>Status</th></tr></thead><tbody>""")
        snapshot.items.forEach {
            append("""<tr class="st-${it.state.name.lowercase()}"><td>${esc(it.countrySlug)}</td><td>${it.locationLevel}</td><td>${label(it.state)}</td></tr>""")
        }
        append("</tbody></table>")
        snapshot.failure?.let { append("""<p class="error">${esc(it)}</p>""") }
        snapshot.result?.let { r ->
            append("""<p class="done">Done: ${r.totalCreated} created, ${r.totalUpdated} updated.""")
            r.reportPdfUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("/") }
                ?.let { append(""" <a href="${esc(it)}" target="_blank" rel="noopener">Open the PDF report</a>""") }
            append("</p>")
        }
        append("</div>")
    }
}
