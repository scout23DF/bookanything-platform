package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryCatalog
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryMention
import de.org.dexterity.bookanything.dom01geolocation.application.companion.ImportIntentValidator
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.InterpretationSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RuleBasedVoiceCommandInterpreterTest {

    private val interpreter = RuleBasedVoiceCommandInterpreter()

    private suspend fun heard(sentence: String) = interpreter.interpret(sentence)?.mentions

    @Test
    fun `english - two countries and two levels`() = runTest {
        assertEquals(
            listOf(CountryMention("DEU", listOf(0, 1)), CountryMention("LUX", listOf(0, 1))),
            heard("Import Germany and Luxembourg level 0 and 1")
        )
    }

    @Test
    fun `portuguese with accents`() = runTest {
        assertEquals(
            listOf(CountryMention("BRA", listOf(2)), CountryMention("PRT", listOf(2))),
            heard("Importa o Brasil e Portugal no nível 2")
        )
    }

    @Test
    fun `german`() = runTest {
        assertEquals(listOf(CountryMention("AUT", listOf(1))), heard("Bitte Österreich importieren, Stufe 1"))
    }

    @Test
    fun `up to a level means from zero`() = runTest {
        assertEquals(listOf(CountryMention("ESP", listOf(0, 1, 2))), heard("importar Espanha até o nível 2"))
        assertEquals(listOf(CountryMention("FRA", listOf(0, 1, 2, 3))), heard("import France up to level 3"))
    }

    @Test
    fun `a range of levels`() = runTest {
        assertEquals(listOf(CountryMention("ITA", listOf(1, 2, 3))), heard("import Italy levels 1 to 3"))
    }

    @Test
    fun `no level word leaves the levels to the validator default`() = runTest {
        assertEquals(listOf(CountryMention("CHL", emptyList())), heard("please import Chile"))
    }

    @Test
    fun `niger is not nigeria and a longer name wins`() = runTest {
        assertEquals(listOf("NER"), heard("import Niger")!!.map { it.country })
        assertEquals(listOf("NGA"), heard("import Nigeria")!!.map { it.country })
        assertEquals(listOf("ZAF"), heard("import South Africa")!!.map { it.country })
    }

    @Test
    fun `an ISO3 code in capitals is accepted but the lower case word is not a country`() = runTest {
        assertEquals(listOf("DEU", "LUX"), heard("DEU and LUX level 0")!!.map { it.country })
        assertNull(heard("what can you do for me"))
    }

    @Test
    fun `the four one-click presets of the UI are understood by the rules (the demo fallback)`() = runTest {
        suspend fun items(sentence: String) = ImportIntentValidator().validate(interpreter.interpret(sentence)!!, InterpretationSource.RULES).items
            .map { it.countrySlug to it.locationLevel }

        assertEquals(listOf("LUX" to 0, "LUX" to 1), items("Import Luxembourg levels 0 and 1"))
        assertEquals(listOf("DEU" to 0, "DEU" to 1, "LUX" to 0, "LUX" to 1), items("Import Germany and Luxembourg level 0 and 1"))
        assertEquals(listOf("DEU" to 0, "DEU" to 1, "LUX" to 0, "LUX" to 1), items("Importiere Deutschland und Luxemburg bis Stufe 1"))
        assertEquals(listOf("PRT" to 0, "PRT" to 1, "LUX" to 0, "LUX" to 1), items("Importar Portugal e Luxemburgo até o nível 1"))
    }

    @Test
    fun `no country - null`() = runTest {
        assertNull(heard("hello, how are you today"))
    }

    @Test
    fun `catalog resolves names and codes and rejects the rest`() {
        assertEquals("DEU", CountryCatalog.resolve("Deutschland"))
        assertEquals("DEU", CountryCatalog.resolve("deu"))
        assertEquals("CHE", CountryCatalog.resolve("Suíça"))
        assertNull(CountryCatalog.resolve("Atlantis"))
        assertNull(CountryCatalog.resolve("ZZZ"))
    }
}
