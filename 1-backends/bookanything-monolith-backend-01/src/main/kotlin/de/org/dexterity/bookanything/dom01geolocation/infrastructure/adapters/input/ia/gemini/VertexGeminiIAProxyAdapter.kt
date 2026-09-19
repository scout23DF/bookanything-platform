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
        var backoffMs = 5000L

        while (retries > 0) {
            val startTime = System.currentTimeMillis()
            try {
                logger.info("VertexGeminiIAProxyAdapter: Calling Gemini API with prompt length: ${promptToSearch.length} chars (Attempts left: $retries)...")
                val response = chatClient.prompt()
                    .user(promptToSearch)
                    .call()
                    .content()

                val duration = System.currentTimeMillis() - startTime
                logger.info("VertexGeminiIAProxyAdapter: Gemini API call succeeded in ${duration}ms. Response size: ${response?.length ?: 0} chars.")
                return response
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val rootCause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(e)
                val causeChain = buildString {
                    var curr: Throwable? = e
                    while (curr != null) {
                        append("[").append(curr.javaClass.simpleName).append(": ").append(curr.message).append("] ")
                        curr = curr.cause
                    }
                }

                val isRateLimit = causeChain.contains("429") ||
                        causeChain.contains("RESOURCE_EXHAUSTED") ||
                        causeChain.contains("quota", ignoreCase = true) ||
                        causeChain.contains("rate", ignoreCase = true)

                if (isRateLimit && retries > 1) {
                    logger.warn("VertexGeminiIAProxyAdapter: Gemini API rate limit / quota exceeded after ${duration}ms! Root cause: [${rootCause.javaClass.simpleName}] ${rootCause.message}. Retrying in ${backoffMs}ms... (Remaining attempts: ${retries - 1})")
                    try {
                        Thread.sleep(backoffMs)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw e
                    }
                    backoffMs *= 2
                    retries--
                } else {
                    logger.error("VertexGeminiIAProxyAdapter: Gemini API failed permanently after ${duration}ms! Root cause: [${rootCause.javaClass.simpleName}] ${rootCause.message}. Cause chain: $causeChain", e)
                    throw e
                }
            }
        }
        return null
    }
}