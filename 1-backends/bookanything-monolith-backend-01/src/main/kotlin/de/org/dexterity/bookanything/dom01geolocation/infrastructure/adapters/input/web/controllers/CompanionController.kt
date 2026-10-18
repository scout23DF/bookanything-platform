package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionInterpretationService
import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionProgressHtml
import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionProgressStreamService
import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionRunRegistry
import de.org.dexterity.bookanything.dom01geolocation.application.companion.ImportIntentValidator
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.BatchImportStarter
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.InterpretationResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class InterpretRequest(val transcript: String = "")
data class ConfirmRequest(val items: List<CountryImportItem> = emptyList())

/**
 * The AI Companion: a sentence in, a confirmed import out, and live progress back.
 *
 * 1. `POST /interpret`: the sentence becomes a validated list of imports. Nothing is started.
 * 2. `POST /confirm`: the user approved that list; it is validated again (the client is not trusted) and only then does the workflow start.
 * 3. `GET /workflows/{id}/stream`: Server-Sent Events with the live progress, `event: progress` while it runs and `event: done` once.
 *
 * Handlers are `suspend` / return a `Flow`, so the servlet thread is released while the model or Temporal answers.
 * Known debt: like the rest of the geolocations API, these are `permitAll()` in `SecurityConfig`; keep them off any exposed network.
 */
@RestController
@RequestMapping("/api/v1/geolocations/companion")
class CompanionController(
    private val interpretation: CompanionInterpretationService,
    private val validator: ImportIntentValidator,
    private val starter: BatchImportStarter,
    private val registry: CompanionRunRegistry,
    private val progress: CompanionProgressStreamService
) {

    @PostMapping("/interpret")
    suspend fun interpret(@RequestBody request: InterpretRequest): InterpretationResult =
        interpretation.interpret(request.transcript)

    @PostMapping("/confirm")
    fun confirm(@RequestBody request: ConfirmRequest): ResponseEntity<Map<String, Any>> {
        val checked = validator.revalidate(request.items)
        if (checked.items.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body(mapOf("error" to "Nothing valid to import", "warnings" to checked.warnings))
        }
        val workflowId = starter.start(GeoLocationBatchImportRequest(items = checked.items))
        registry.remember(workflowId, checked.items)
        return ResponseEntity.accepted().body(
            mapOf(
                "workflowId" to workflowId,
                "items" to checked.items,
                "streamUrl" to "/api/v1/geolocations/companion/workflows/$workflowId/stream"
            )
        )
    }

    @GetMapping("/workflows/{workflowId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@PathVariable workflowId: String): Flow<ServerSentEvent<String>> {
        val items = registry.itemsOf(workflowId).orEmpty()
        return progress.stream(workflowId, items).map { snapshot ->
            ServerSentEvent.builder(CompanionProgressHtml.render(snapshot))
                .event(if (snapshot.terminal) "done" else "progress")
                .build()
        }
    }
}
