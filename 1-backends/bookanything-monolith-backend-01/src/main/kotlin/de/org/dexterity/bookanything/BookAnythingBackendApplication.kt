package de.org.dexterity.bookanything

import org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.web.config.EnableSpringDataWebSupport

@SpringBootApplication(exclude = [VertexAiGeminiAutoConfiguration::class])
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class BookAnythingBackendApplication

fun main(args: Array<String>) {
	runApplication<BookAnythingBackendApplication>(*args)
}
