package de.org.dexterity.bookanything

import org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.data.elasticsearch.ElasticsearchReactiveHealthContributorAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticsearchRestHealthContributorAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.web.config.EnableSpringDataWebSupport

@SpringBootApplication(
	exclude = [
		VertexAiGeminiAutoConfiguration::class,
		ElasticsearchRestHealthContributorAutoConfiguration::class,
		ElasticsearchReactiveHealthContributorAutoConfiguration::class
	]
)
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
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
