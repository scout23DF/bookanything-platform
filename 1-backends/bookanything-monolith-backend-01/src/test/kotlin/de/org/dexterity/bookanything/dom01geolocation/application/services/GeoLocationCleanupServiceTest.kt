package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItem
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ContinentModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.CountryModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ProvinceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.RegionModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.CorporateStoragePort
import de.org.dexterity.bookanything.dom02assetmanager.application.services.AssetCRUDService
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetStatus
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.BucketModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.StorageProviderType
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.transaction.PlatformTransactionManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GeoLocationCleanupServiceTest {

    private val crud = mockk<GeoLocationCRUDService>(relaxed = true)
    private val assets = mockk<AssetPersistRepositoryPort>(relaxed = true)
    private val assetCrud = mockk<AssetCRUDService>(relaxed = true)
    private val corporate = mockk<CorporateStoragePort>(relaxed = true)
    private val service = GeoLocationCleanupService(
        crud, assets, assetCrud, corporate, mockk<PlatformTransactionManager>(relaxed = true), "geodata", "reports", 4
    )

    private val continent = ContinentModel(GeoLocationId(1), "SA", "Americas")
    private val region = RegionModel(GeoLocationId(2), "SAM", "South America", parentId = 1, continent = continent)
    private val country = CountryModel(GeoLocationId(54), "BRA", "Brazil", parentId = 2, region = region)
    private fun province(id: Long, code: String) =
        ProvinceModel(GeoLocationId(id), code, code, parentId = 54, country = country)
    private val provinces = listOf(province(177, "BR-AC"), province(178, "BR-AL"))
    private val bucket = BucketModel(1, "bookanything-images", StorageProviderType.MINIO)
    private fun asset(id: Long, key: String) =
        AssetModel(id = id, bucket = bucket, fileName = key.substringAfterLast('/'), storageKey = key, mimeType = "x",
            size = 1, category = AssetCategory.DOCUMENT, status = AssetStatus.AVAILABLE)

    @BeforeEach
    fun setUp() {
        every { crud.findByFriendlyId(GeoLocationType.COUNTRY, "BRA", any()) } returns PageImpl(listOf(country))
        every { crud.searchByParentIdAndNameStartingWith(GeoLocationType.PROVINCE, 54, "", any()) } returns PageImpl(provinces)
        every { assets.findByStorageKeyStartingWith(any(), any()) } returns PageImpl(emptyList())
        every { corporate.exists(any(), any()) } returns false
    }

    @Test
    fun `accepts an ISO3 code with level 0 or 1 and rejects anything else`() {
        assertTrue(service.validate(CleanupItem("BRA", 0)).isEmpty())
        assertTrue(service.validate(CleanupItem("bra", 1)).isEmpty())
        assertEquals(1, service.validate(CleanupItem("BR", 0)).size)
        assertEquals(1, service.validate(CleanupItem("B1A", 1)).size)
        assertEquals(1, service.validate(CleanupItem("BRA", 2)).size)
        assertEquals(2, service.validate(CleanupItem("BRASIL", 5)).size)
    }

    @Test
    fun `the corporate report key follows the alias, then the friendly id, replacing slashes`() {
        assertEquals("geolocations/geolocation-detail-report-BR-AC.pdf", GeoLocationCleanupService.corporateReportKey(provinces[0]))
        val withAlias = ProvinceModel(GeoLocationId(9), "X/Y", "n", alias = "A/B", parentId = 54, country = country)
        assertEquals("geolocations/geolocation-detail-report-A-B.pdf", GeoLocationCleanupService.corporateReportKey(withAlias))
    }

    @Test
    fun `plan of level 1 lists the provinces, their assets and the source files, changing nothing`() {
        every { assets.findByStorageKeyStartingWith("images/geolocations/177/", any()) } returns
            PageImpl(listOf(asset(1, "images/geolocations/177/map.svg"), asset(2, "images/geolocations/177/flag.svg")))
        every { assets.findByStorageKeyStartingWith("documents/geolocations/177/", any()) } returns
            PageImpl(listOf(asset(3, "documents/geolocations/177/report.pdf")))
        every { corporate.exists("reports", "geolocations/geolocation-detail-report-BR-AC.pdf") } returns true
        every { corporate.exists("geodata", "raw/gadm41_BRA_1.json") } returns true

        val plan = service.plan(CleanupItem("bra", 1, shouldDeleteJsonFile = true))

        assertEquals("PROVINCE", plan.targetType)
        assertEquals(listOf(177L, 178L), plan.geoLocationIds)
        assertEquals(3, plan.assetCount)
        assertEquals(1, plan.corporateReportCount)
        assertEquals(2, plan.childProvinceCount)
        assertTrue(plan.jsonFileExists); assertFalse(plan.xmlFileExists)
        assertTrue(plan.willDeleteJsonFile); assertFalse(plan.willDeleteXmlFile)
        assertTrue(plan.blockers.isEmpty())
        verify(exactly = 0) { crud.deleteById(any(), any()) }
        verify(exactly = 0) { corporate.delete(any(), any()) }
        coVerify(exactly = 0) { assetCrud.deleteAsset(any()) }
    }

    @Test
    fun `plan of level 0 targets the country and reports how many provinces it still has`() {
        val plan = service.plan(CleanupItem("BRA", 0))
        assertEquals("COUNTRY", plan.targetType)
        assertEquals(listOf(54L), plan.geoLocationIds)
        assertEquals(2, plan.childProvinceCount)
    }

    @Test
    fun `plan of an invalid item is blocked without reading the database`() {
        val plan = service.plan(CleanupItem("BR", 7))
        assertEquals(2, plan.blockers.size)
        verify(exactly = 0) { crud.findByFriendlyId(any(), any(), any()) }
    }

    @Test
    fun `plan of a country that is not in the database is empty and not blocked`() {
        every { crud.findByFriendlyId(GeoLocationType.COUNTRY, "PRT", any()) } returns PageImpl(emptyList())
        val plan = service.plan(CleanupItem("PRT", 1))
        assertTrue(plan.geoLocationIds.isEmpty()); assertTrue(plan.blockers.isEmpty())
    }

    @Test
    fun `deleting level 1 removes assets, then the report copy, then the province, one province at a time`() {
        every { assets.findByStorageKeyStartingWith("images/geolocations/177/", any()) } returns PageImpl(listOf(asset(1, "images/geolocations/177/map.svg")))
        every { assets.findByStorageKeyStartingWith("documents/geolocations/177/", any()) } returns PageImpl(listOf(asset(3, "documents/geolocations/177/r.pdf")))
        coEvery { assetCrud.deleteAsset(any()) } returns true
        every { corporate.exists("reports", "geolocations/geolocation-detail-report-BR-AC.pdf") } returns true

        val summary = service.deleteData(CleanupItem("BRA", 1))

        assertEquals(2, summary.deletedGeoLocations)
        assertEquals(2, summary.deletedAssets)
        assertEquals(1, summary.deletedCorporateReports)
        coVerify { assetCrud.deleteAsset(1) }
        coVerify { assetCrud.deleteAsset(3) }
        verifyOrder {
            corporate.delete("reports", "geolocations/geolocation-detail-report-BR-AC.pdf")
            crud.deleteById(GeoLocationType.PROVINCE, 177)
            crud.deleteById(GeoLocationType.PROVINCE, 178)
        }
    }

    @Test
    fun `only countries and provinces are ever deleted, never continents or regions`() {
        service.deleteData(CleanupItem("BRA", 1))
        service.deleteData(CleanupItem("BRA", 0))

        verify { crud.deleteById(GeoLocationType.PROVINCE, any()) }
        verify { crud.deleteById(GeoLocationType.COUNTRY, 54) }
        verify(exactly = 0) { crud.deleteById(GeoLocationType.CONTINENT, any()) }
        verify(exactly = 0) { crud.deleteById(GeoLocationType.REGION, any()) }
        verify(exactly = 0) { crud.deleteAll(any()) }
        verify(exactly = 0) { crud.deleteByParentId(any(), any()) }
    }

    @Test
    fun `the job summaries are never touched, only the detail report of each GeoLocation`() {
        every { corporate.exists(any(), any()) } returns true
        service.deleteData(CleanupItem("BRA", 1))
        verify { corporate.delete("reports", match { it.startsWith("geolocations/geolocation-detail-report-") }) }
        verify(exactly = 0) { corporate.delete(any(), match { it.contains("summary") }) }
        verify(exactly = 0) { corporate.delete("geodata", any()) }  // the source files belong to the NiFi flow
    }

    @Test
    fun `an invalid item deletes nothing`() {
        assertFailsWith<IllegalArgumentException> { service.deleteData(CleanupItem("BR", 0)) }
        verify(exactly = 0) { crud.deleteById(any(), any()) }
    }

    @Test
    fun `remaining counts what is still in the database`() {
        every { assets.findByStorageKeyStartingWith("images/geolocations/177/", any()) } returns PageImpl(listOf(asset(1, "images/geolocations/177/m.svg")))
        assertEquals(Pair(2, 1), service.remaining(CleanupItem("BRA", 1)))
    }

    @Test
    fun `deleteAssetsConcurrently deletes assets in parallel waves bounded by Semaphore`() = runTest {
        val inFlight = AtomicInteger(0)
        val peakInFlight = AtomicInteger(0)

        coEvery { assetCrud.deleteAsset(any()) } coAnswers {
            val current = inFlight.incrementAndGet()
            peakInFlight.updateAndGet { maxOf(it, current) }
            try {
                delay(1_000)
                true
            } finally {
                inFlight.decrementAndGet()
            }
        }

        // 8 assets, each taking 1,000ms virtual time, with Semaphore(4) -> 2 waves = 2,000ms virtual time (not 8,000ms)
        val eightAssets = (1L..8L).map { asset(it, "images/geolocations/177/file-$it.svg") }
        val deletedCount = service.deleteAssetsConcurrently(eightAssets)

        assertEquals(8, deletedCount)
        assertEquals(2_000L, currentTime, "8 deletes of 1,000ms with Semaphore(4) must complete in 2 waves (2,000ms virtual time)")
        assertEquals(4, peakInFlight.get(), "Concurrency must be capped at 4 by Semaphore")
    }
}
