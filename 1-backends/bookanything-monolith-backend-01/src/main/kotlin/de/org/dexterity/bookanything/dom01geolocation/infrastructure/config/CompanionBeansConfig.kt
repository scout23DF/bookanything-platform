package de.org.dexterity.bookanything.dom01geolocation.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionInterpretationService
import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionProgressStreamService
import de.org.dexterity.bookanything.dom01geolocation.application.companion.CompanionRunRegistry
import de.org.dexterity.bookanything.dom01geolocation.application.companion.ImportIntentValidator
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowProgressPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion.GeminiVoiceCommandInterpreter
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion.RuleBasedVoiceCommandInterpreter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CompanionBeansConfig {

    @Bean
    fun importIntentValidator(
        @Value("\${application.domain-settings.geolocation.companion.max-items:6}") maxItems: Int
    ) = ImportIntentValidator(maxItems)

    @Bean
    fun companionInterpretationService(
        gemini: SearchEngineInIAProxyPort,
        objectMapper: ObjectMapper,
        validator: ImportIntentValidator,
        @Value("\${application.domain-settings.geolocation.companion.gemini-budget-ms:5000}") budgetMs: Long
    ) = CompanionInterpretationService(
        primary = GeminiVoiceCommandInterpreter(gemini, objectMapper),
        fallback = RuleBasedVoiceCommandInterpreter(),
        validator = validator,
        primaryBudgetMs = budgetMs
    )

    @Bean
    fun companionProgressStreamService(
        port: WorkflowProgressPort,
        @Value("\${application.domain-settings.geolocation.companion.poll-interval-ms:1000}") pollMs: Long
    ) = CompanionProgressStreamService(port, pollIntervalMs = pollMs)

    @Bean
    fun companionRunRegistry() = CompanionRunRegistry()
}
