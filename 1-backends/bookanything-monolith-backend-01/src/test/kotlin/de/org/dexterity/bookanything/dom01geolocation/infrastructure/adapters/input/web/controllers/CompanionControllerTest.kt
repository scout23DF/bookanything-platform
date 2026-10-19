package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionInterpretationService
import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionProgressStreamService
import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionRunRegistry
import de.org.dexterity.bookanything.dom01geolocation.application.companion.ImportIntentValidator
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.BatchImportStarter
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.RawIntent
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.VoiceCommandInterpreterPort
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowPoll
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowProgressPort
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowRunState
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion.RuleBasedVoiceCommandInterpreter
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import kotlin.test.assertEquals

/** The HTTP contract of the Companion: suspend handlers, validation on confirm, and the SSE stream. */
class CompanionControllerTest {

    private val neverGemini = object : VoiceCommandInterpreterPort {
        override suspend fun interpret(transcript: String): RawIntent? = null
    }

    private val starter = mockk<BatchImportStarter>()
    private val registry = CompanionRunRegistry()
    private val port = object : WorkflowProgressPort {
        private var n = 0
        override suspend fun poll(workflowId: String) =
            if (n++ == 0) WorkflowPoll(ImportProgressDto(currentStage = "NIFI_INGESTION", percentCompleted = 20, processedItems = 0, totalItems = 1), WorkflowRunState.RUNNING)
            else WorkflowPoll(ImportProgressDto(currentStage = "COMPLETED", percentCompleted = 100, processedItems = 1, totalItems = 1, status = "COMPLETED"), WorkflowRunState.COMPLETED)
        override suspend fun result(workflowId: String) = GeoLocationBatchImportResult(totalCreated = 3, reportPdfUrl = "https://example.org/r.pdf")
    }

    private val mvc: MockMvc = MockMvcBuilders.standaloneSetup(
        CompanionController(
            CompanionInterpretationService(neverGemini, RuleBasedVoiceCommandInterpreter(), ImportIntentValidator()),
            ImportIntentValidator(), starter, registry,
            CompanionProgressStreamService(port, pollIntervalMs = 20)
        )
    ).setMessageConverters(MappingJackson2HttpMessageConverter(com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules())).build()

    @Test
    fun `interpret - a sentence becomes a validated list and nothing is started`() {
        val started = mvc.perform(post("/api/v1/geolocations/companion/interpret").contentType(MediaType.APPLICATION_JSON).content("""{"transcript":"Import Germany and Luxembourg level 0 and 1"}"""))
            .andExpect(request().asyncStarted()).andReturn()

        mvc.perform(asyncDispatch(started))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.source").value("RULES"))
            .andExpect(jsonPath("$.items.length()").value(4))
            .andExpect(jsonPath("$.items[0].countrySlug").value("DEU"))
    }

    @Test
    fun `confirm - starts the workflow with the revalidated items only`() {
        val sent = slot<GeoLocationBatchImportRequest>()
        every { starter.start(capture(sent)) } returns "geo-import-1"

        mvc.perform(post("/api/v1/geolocations/companion/confirm").contentType(MediaType.APPLICATION_JSON)
            .content("""{"items":[{"countrySlug":"DEU","locationLevel":1},{"countrySlug":"EVIL","locationLevel":0},{"countrySlug":"LUX","locationLevel":9}]}"""))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.workflowId").value("geo-import-1"))
            .andExpect(jsonPath("$.streamUrl").value("/api/v1/geolocations/companion/workflows/geo-import-1/stream"))

        assertEquals(listOf(CountryImportItem("DEU", 1)), sent.captured.items)
        assertEquals(listOf(CountryImportItem("DEU", 1)), registry.itemsOf("geo-import-1"))
    }

    @Test
    fun `confirm - nothing valid is a 422 and no workflow`() {
        mvc.perform(post("/api/v1/geolocations/companion/confirm").contentType(MediaType.APPLICATION_JSON).content("""{"items":[{"countrySlug":"XXX","locationLevel":0}]}"""))
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `stream - server-sent events, progress then done`() {
        registry.remember("wf-1", listOf(CountryImportItem("DEU", 0)))

        val started = mvc.perform(get("/api/v1/geolocations/companion/workflows/wf-1/stream").accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(request().asyncStarted()).andReturn()
        started.asyncResult // blocks until the flow completes
        val body = mvc.perform(asyncDispatch(started)).andExpect(status().isOk).andReturn().response.contentAsString

        val events = body.lines().filter { it.startsWith("event:") }.map { it.removePrefix("event:").trim() }
        assertEquals(listOf("progress", "done"), events)
        assert(body.contains("Ingesting via NiFi"))
        assert(body.contains("Open the PDF report"))
    }

    private fun confirmedWith(body: String): GeoLocationBatchImportRequest {
        val sent = slot<GeoLocationBatchImportRequest>()
        every { starter.start(capture(sent)) } returns "geo-import-2"

        mvc.perform(post("/api/v1/geolocations/companion/confirm").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isAccepted)
        return sent.captured
    }

    @Test
    fun `confirm - without a generator the workflow gets GEOPANDAS`() {
        assertEquals("GEOPANDAS", confirmedWith("""{"items":[{"countrySlug":"DEU","locationLevel":1}]}""").mapSvgGeneratorMechanism)
    }

    @Test
    fun `confirm - the generator the user asked for reaches the workflow`() {
        assertEquals("KOTLIN", confirmedWith("""{"items":[{"countrySlug":"DEU","locationLevel":1}],"mapSvgGeneratorMechanism":"KOTLIN"}""").mapSvgGeneratorMechanism)
    }

    @Test
    fun `confirm - an unknown generator falls back to GEOPANDAS`() {
        assertEquals("GEOPANDAS", confirmedWith("""{"items":[{"countrySlug":"DEU","locationLevel":1}],"mapSvgGeneratorMechanism":"PERL"}""").mapSvgGeneratorMechanism)
    }

    @Test
    fun `interpret - a spoken generator is returned for the confirmation screen`() {
        val started = mvc.perform(post("/api/v1/geolocations/companion/interpret").contentType(MediaType.APPLICATION_JSON).content("""{"transcript":"Import Chile with Kotlin"}"""))
            .andExpect(request().asyncStarted()).andReturn()

        mvc.perform(asyncDispatch(started))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mapSvgGeneratorMechanism").value("KOTLIN"))
    }

    @Test
    fun `interpret - saying nothing about the generator returns GEOPANDAS`() {
        val started = mvc.perform(post("/api/v1/geolocations/companion/interpret").contentType(MediaType.APPLICATION_JSON).content("""{"transcript":"Import Chile level 1"}"""))
            .andExpect(request().asyncStarted()).andReturn()

        mvc.perform(asyncDispatch(started))
            .andExpect(jsonPath("$.mapSvgGeneratorMechanism").value("GEOPANDAS"))
    }
}
