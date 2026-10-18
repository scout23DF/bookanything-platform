package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryMention
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GeminiVoiceCommandInterpreterTest {

    private fun interpreter(answer: String?) = GeminiVoiceCommandInterpreter(
        object : SearchEngineInIAProxyPort {
            override fun simpleSearchByPrompt(promptToSearch: String) = answer
        },
        ObjectMapper()
    )

    @Test
    fun `reads the JSON object, also when the model wraps it in markdown`() = runTest {
        val answer = "```json\n{\"items\":[{\"country\":\"Germany\",\"levels\":[0,1]},{\"country\":\"Luxembourg\",\"levels\":[]}]}\n```"

        assertEquals(
            listOf(CountryMention("Germany", listOf(0, 1)), CountryMention("Luxembourg", emptyList())),
            interpreter(answer).interpret("x")!!.mentions
        )
    }

    @Test
    fun `garbage, a missing items array and no answer are all null so the rules take over`() = runTest {
        assertNull(interpreter("I am sorry, I cannot help").interpret("x"))
        assertNull(interpreter("{\"foo\":1}").interpret("x"))
        assertNull(interpreter("{\"items\": [").interpret("x"))
        assertNull(interpreter(null).interpret("x"))
    }

    @Test
    fun `entries without a country are skipped and non numeric levels ignored`() = runTest {
        val answer = "{\"items\":[{\"levels\":[1]},{\"country\":\"Chile\",\"levels\":[\"a\",2]}]}"

        assertEquals(listOf(CountryMention("Chile", listOf(2))), interpreter(answer).interpret("x")!!.mentions)
    }
}
