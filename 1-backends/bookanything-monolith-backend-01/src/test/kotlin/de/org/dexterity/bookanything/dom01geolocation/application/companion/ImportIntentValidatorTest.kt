package de.org.dexterity.bookanything.dom01geolocation.application.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.MapSvgMechanism
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryMention
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.InterpretationSource
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.RawIntent
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportIntentValidatorTest {

    private val validator = ImportIntentValidator(maxItems = 4)

    private fun intent(vararg m: CountryMention) = RawIntent(m.toList())

    @Test
    fun `names become ISO3 slugs, duplicates collapse, no level means 0`() {
        val r = validator.validate(intent(CountryMention("Germany", listOf(1, 1)), CountryMention("Luxemburgo", emptyList())), InterpretationSource.GEMINI)

        assertEquals(listOf(CountryImportItem("DEU", 1), CountryImportItem("LUX", 0)), r.items)
        assertEquals(InterpretationSource.GEMINI, r.source)
    }

    @Test
    fun `an unknown country is dropped with a warning, it never reaches the import`() {
        val r = validator.validate(intent(CountryMention("Atlantis", listOf(0)), CountryMention("Chile", listOf(0))), InterpretationSource.GEMINI)

        assertEquals(listOf(CountryImportItem("CHL", 0)), r.items)
        assertTrue(r.warnings.single().contains("Atlantis"))
    }

    @Test
    fun `levels outside 0 to 3 are dropped`() {
        val r = validator.validate(intent(CountryMention("Chile", listOf(-1, 2, 4, 99))), InterpretationSource.RULES)

        assertEquals(listOf(CountryImportItem("CHL", 2)), r.items)
        assertEquals(3, r.warnings.size)
    }

    @Test
    fun `a model that asks for the whole world is capped`() {
        val r = validator.validate(intent(CountryMention("Germany", listOf(0, 1, 2, 3)), CountryMention("France", listOf(0, 1, 2, 3))), InterpretationSource.GEMINI)

        assertEquals(4, r.items.size)
        assertTrue(r.warnings.any { it.contains("first 4 of 8") })
    }

    @Test
    fun `nothing valid - source NONE`() {
        val r = validator.validate(intent(CountryMention("Atlantis", listOf(0))), InterpretationSource.GEMINI)

        assertTrue(r.items.isEmpty())
        assertEquals(InterpretationSource.NONE, r.source)
    }

    @Test
    fun `revalidate does not trust what the client sends back`() {
        val r = validator.revalidate(listOf(CountryImportItem("DEU", 0), CountryImportItem("XXX", 0), CountryImportItem("DEU", 7), CountryImportItem("../etc", 1)))

        assertEquals(listOf(CountryImportItem("DEU", 0)), r.items)
    }

    @Test
    fun `no generator requested - GeoPandas is the default`() {
        val r = validator.validate(RawIntent(listOf(CountryMention("Chile", listOf(0)))), InterpretationSource.GEMINI)

        assertEquals(MapSvgMechanism.GEOPANDAS, r.mapSvgGeneratorMechanism)
        assertTrue(r.warnings.isEmpty())
    }

    @Test
    fun `a requested generator is used, in any case`() {
        val kotlin = validator.validate(RawIntent(listOf(CountryMention("Chile", listOf(0))), "kotlin"), InterpretationSource.RULES)
        val pandas = validator.validate(RawIntent(listOf(CountryMention("Chile", listOf(0))), " GeoPandas "), InterpretationSource.RULES)

        assertEquals(MapSvgMechanism.KOTLIN, kotlin.mapSvgGeneratorMechanism)
        assertEquals(MapSvgMechanism.GEOPANDAS, pandas.mapSvgGeneratorMechanism)
    }

    @Test
    fun `an unknown generator is ignored with a warning, never passed on`() {
        val r = validator.validate(RawIntent(listOf(CountryMention("Chile", listOf(0))), "rm -rf"), InterpretationSource.GEMINI)

        assertEquals(MapSvgMechanism.GEOPANDAS, r.mapSvgGeneratorMechanism)
        assertTrue(r.warnings.single().contains("Unknown map generator"))
    }

    @Test
    fun `revalidate keeps the generator the user confirmed`() {
        val r = validator.revalidate(listOf(CountryImportItem("CHL", 0)), "KOTLIN")

        assertEquals(MapSvgMechanism.KOTLIN, r.mapSvgGeneratorMechanism)
    }
}
