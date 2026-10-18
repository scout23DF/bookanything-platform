package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.ItemState
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.StandardCharsets

class CompanionUiResourcesTest {

    @Test
    fun `companion index html and offline vendored htmx sse assets are bundled on classpath`() {
        val indexResource = ClassPathResource("public/companion/index.html")
        val htmxResource = ClassPathResource("public/companion/htmx.min.js")
        val sseResource = ClassPathResource("public/companion/sse.js")

        assertTrue(indexResource.exists(), "public/companion/index.html must exist on classpath")
        assertTrue(htmxResource.exists(), "public/companion/htmx.min.js must exist on classpath")
        assertTrue(sseResource.exists(), "public/companion/sse.js must exist on classpath")

        val htmxBytes = htmxResource.inputStream.use { it.readAllBytes() }
        val sseBytes = sseResource.inputStream.use { it.readAllBytes() }
        assertTrue(htmxBytes.size > 10_000, "htmx.min.js should be non-empty vendored bundle")
        assertTrue(sseBytes.size > 1_000, "sse.js should be non-empty vendored bundle")
    }

    @Test
    fun `companion index html matches CompanionController endpoints and HTMX SSE progress contract`() {
        val indexHtml = ClassPathResource("public/companion/index.html")
            .inputStream.use { String(it.readAllBytes(), StandardCharsets.UTF_8) }

        // Local vendored script references (no CDN dependency during live demo)
        assertTrue(indexHtml.contains("src=\"/companion/htmx.min.js\""))
        assertTrue(indexHtml.contains("src=\"/companion/sse.js\""))

        // Guardrail 2-step endpoints: interpret -> confirm
        assertTrue(indexHtml.contains("/api/v1/geolocations/companion/interpret"))
        assertTrue(indexHtml.contains("/api/v1/geolocations/companion/confirm"))

        // HTMX SSE extension attributes matching CompanionController stream events ("progress" and "done")
        assertTrue(indexHtml.contains("hx-ext=\"sse\""))
        assertTrue(indexHtml.contains("sse-connect="))
        assertTrue(indexHtml.contains("sse-swap=\"progress,done\""))
        assertTrue(indexHtml.contains("sse-close=\"done\""))

        // Web Speech API + one-click interview presets
        assertTrue(indexHtml.contains("SpeechRecognition"))
        assertTrue(indexHtml.contains("webkitSpeechRecognition"))
        assertTrue(indexHtml.contains("data-transcript=\"Import Luxembourg levels 0 and 1\""))
        assertTrue(indexHtml.contains("data-transcript=\"Importiere Deutschland und Luxemburg bis Stufe 1\""))

        // CSS selectors for every ItemState emitted by CompanionProgressHtml.render()
        for (state in ItemState.entries) {
            val expectedClass = "st-${state.name.lowercase()}"
            assertTrue(
                indexHtml.contains(expectedClass),
                "index.html should style row class '$expectedClass' emitted by CompanionProgressHtml"
            )
        }
    }

    @Test
    fun `graalvm native resource config includes public static assets`() {
        val resourceConfig = ClassPathResource("META-INF/native-image/resource-config.json")
            .inputStream.use { String(it.readAllBytes(), StandardCharsets.UTF_8) }

        assertNotNull(resourceConfig)
        assertTrue(
            resourceConfig.contains("\"pattern\": \"public/.*\""),
            "resource-config.json must include public/.* for GraalVM native image packaging"
        )
    }
}
