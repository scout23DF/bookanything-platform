package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.wikidata

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CountryModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ProvinceModel
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WikidataGeoResolverTest {

    private val germany = CountryModel(
        id = GeoLocationId(1), friendlyId = "DEU", name = "Germany", alias = "DEU",
        parentId = null, region = mockk(relaxed = true)
    )

    private fun province(alias: String) = ProvinceModel(
        id = GeoLocationId(2), friendlyId = alias, name = "x", alias = alias,
        parentId = 1, country = germany
    )

    @Test
    fun `commons URL hash directories match the ones Wikimedia actually serves`() {
        // Expected values from the Commons imageinfo API (2026-09-23). Several of these were
        // wrong in the old hand-written URL tables.
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/b/be/Flag_of_Goi%C3%A1s.svg",
            WikidataGeoResolver.commonsFileUrl("Flag of Goiás.svg")
        )
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/0/0e/Flag_of_Bremen.svg",
            WikidataGeoResolver.commonsFileUrl("Flag of Bremen.svg")
        )
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/b/bd/Flag_of_Thuringia.svg",
            WikidataGeoResolver.commonsFileUrl("Flag_of_Thuringia.svg")
        )
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/5/5c/Flag_of_Baden-W%C3%BCrttemberg.svg",
            WikidataGeoResolver.commonsFileUrl("Flag of Baden-Württemberg.svg")
        )
    }

    @Test
    fun `GADM aliases map to the right Wikidata ISO statement`() {
        assertEquals("P298" to "DEU", WikidataGeoResolver.isoStatementFor(germany))
        assertEquals("P297" to "DE", WikidataGeoResolver.isoStatementFor(germany.copy(alias = "de")))
        assertEquals("P300" to "DE-BW", WikidataGeoResolver.isoStatementFor(province("DE.BW")))
        assertEquals("P300" to "DE-BY", WikidataGeoResolver.isoStatementFor(province("DE-BY")))
        assertEquals("P300" to "BR-SP", WikidataGeoResolver.isoStatementFor(province("br.sp")))
    }

    @Test
    fun `codes that are not ISO shaped are not looked up`() {
        assertNull(WikidataGeoResolver.isoStatementFor(germany.copy(alias = "GERMANY")))
        assertNull(WikidataGeoResolver.isoStatementFor(province("DE.BW.1_1")))
    }
}
