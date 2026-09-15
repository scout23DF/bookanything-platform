package de.org.dexterity.bookanything

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.web.config.EnableSpringDataWebSupport

import org.springframework.context.annotation.ImportRuntimeHints
import de.org.dexterity.bookanything.wire.nativehints.NativeRuntimeHints

@SpringBootApplication(
	excludeName = [
		"org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration",
		"org.springframework.boot.elasticsearch.autoconfigure.health.ElasticsearchRestHealthContributorAutoConfiguration",
		"org.springframework.boot.data.elasticsearch.autoconfigure.health.DataElasticsearchReactiveHealthContributorAutoConfiguration"
	]
)
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@ImportRuntimeHints(NativeRuntimeHints::class)
class BookAnythingBackendApplication

@org.springframework.web.bind.annotation.RestController
class PipelineVerificationController {
	@org.springframework.web.bind.annotation.GetMapping("/api/cicd/status")
	fun status(): Map<String, String> = mapOf(
		"service" to "bookanything-monolith-backend-01",
		"ci" to "Tekton Pipelines",
		"cd" to "ArgoCD",
		"status" to "verified"
	)
}

fun main(args: Array<String>) {
	runApplication<BookAnythingBackendApplication>(*args)
}
