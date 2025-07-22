package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.ia.gemini

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import org.springframework.ai.chat.client.ChatClient

class VertexGeminiIAProxyAdapter(
    private val chatClient: ChatClient
) : SearchEngineInIAProxyPort {

    override fun simpleSearchByPrompt(promptToSearch: String): String? {

        return chatClient.prompt()
            .user(promptToSearch)
            .call()
            .content()

    }
}