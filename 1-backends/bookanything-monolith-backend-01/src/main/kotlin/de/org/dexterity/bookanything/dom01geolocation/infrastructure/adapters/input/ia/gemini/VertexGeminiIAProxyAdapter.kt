package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.ia.gemini

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import org.springframework.ai.chat.client.ChatClient

import org.slf4j.LoggerFactory

class VertexGeminiIAProxyAdapter(
    private val chatClient: ChatClient
) : SearchEngineInIAProxyPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun simpleSearchByPrompt(promptToSearch: String): String? {
        var retries = 3
        var backoffMs = 4000L

        while (retries > 0) {
            try {
                return chatClient.prompt()
                    .user(promptToSearch)
                    .call()
                    .content()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val isRateLimit = msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("quota", ignoreCase = true)
                if (isRateLimit && retries > 1) {
                    logger.warn("VertexGeminiIAProxyAdapter: Rate limit / quota exceeded (429). Retrying in ${backoffMs}ms... (Remaining retries: ${retries - 1})")
                    try {
                        Thread.sleep(backoffMs)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw e
                    }
                    backoffMs *= 2
                    retries--
                } else {
                    throw e
                }
            }
        }
        return null
    }
}