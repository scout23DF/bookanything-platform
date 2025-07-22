package de.org.dexterity.bookanything.dom01geolocation.domain.ports

interface SearchEngineInIAProxyPort {
    fun simpleSearchByPrompt(promptToSearch: String): String?
}