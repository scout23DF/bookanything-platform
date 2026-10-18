package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.models.ContinentModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.CountryModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.RegionModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.EventPublisherPort
import de.org.dexterity.bookanything.dom02assetmanager.application.services.AssetCRUDService
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetStatus
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.BucketModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.StorageProviderType
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.BucketPersistRepositoryPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.StorageProviderPort
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.UploadResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.transaction.annotation.Transactional
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.full.findAnnotation
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GeoLocationAssetIntegrationConcurrencyTest {

    private val storageProvider = mockk<StorageProviderPort>(relaxed = true)
    private val assetRepository = mockk<AssetPersistRepositoryPort>(relaxed = true)
    private val bucketRepository = mockk<BucketPersistRepositoryPort>(relaxed = true)
    private val eventPublisher = mockk<EventPublisherPort>(relaxed = true)

    private val bucket = BucketModel(1L, GeoLocationAssetIntegrationService.IMAGES_BUCKET_NAME, StorageProviderType.MINIO)
    private val continent = ContinentModel(GeoLocationId(1), "EU", "Europe")
    private val region = RegionModel(GeoLocationId(2), "WEU", "Western Europe", parentId = 1, continent = continent)
    private val country = CountryModel(GeoLocationId(49), "DEU", "Germany", parentId = 2, region = region)

    @Test
    fun `saveGeoLocationMapSvgs uploads local and world SVGs concurrently in 1x virtual time`() = runTest {
        every { bucketRepository.findByName(GeoLocationAssetIntegrationService.IMAGES_BUCKET_NAME) } returns Optional.of(bucket)
        every { assetRepository.findByStorageKeyStartingWith(any(), any()) } returns PageImpl(emptyList())
        every { assetRepository.save(any()) } answers {
            val arg = firstArg<AssetModel>()
            arg.copy(id = if (arg.storageKey.contains("map-")) 101L else 102L)
        }

        coEvery { storageProvider.upload(any(), any(), any(), any(), any()) } coAnswers {
            delay(1_000)
            UploadResult(secondArg())
        }

        val service = GeoLocationAssetIntegrationService(storageProvider, assetRepository, bucketRepository)
        val (localAsset, worldAsset) = service.saveGeoLocationMapSvgs(country, "<svg>local</svg>", "<svg>world</svg>")

        assertEquals(101L, localAsset.id)
        assertEquals(102L, worldAsset.id)
        // Two 1,000ms uploads running concurrently via async/awaitAll must complete in 1,000ms virtual time (not 2,000ms)
        assertEquals(1_000L, currentTime, "Both SVG uploads must execute concurrently in 1,000ms virtual time")
        coVerify(exactly = 2) { storageProvider.upload(any(), any(), any(), any(), any()) }
        verify(exactly = 2) { assetRepository.save(any()) }
    }

    @Test
    fun `saveGeoLocationMapSvgs cancels sibling upload immediately on failure and writes no DB rows`() = runTest {
        every { bucketRepository.findByName(GeoLocationAssetIntegrationService.IMAGES_BUCKET_NAME) } returns Optional.of(bucket)
        val worldUploadCancelled = AtomicBoolean(false)

        coEvery { storageProvider.upload(any(), match { it.contains("map-") }, any(), any(), any()) } coAnswers {
            delay(300)
            throw IllegalStateException("MinIO network error on local map")
        }
        coEvery { storageProvider.upload(any(), match { it.contains("world-") }, any(), any(), any()) } coAnswers {
            try {
                delay(5_000)
                UploadResult("world")
            } catch (e: CancellationException) {
                worldUploadCancelled.set(true)
                throw e
            }
        }

        val service = GeoLocationAssetIntegrationService(storageProvider, assetRepository, bucketRepository)

        assertFailsWith<IllegalStateException> {
            service.saveGeoLocationMapSvgs(country, "<svg>local</svg>", "<svg>world</svg>")
        }

        assertTrue(worldUploadCancelled.get(), "Sibling upload must be cancelled when local upload fails")
        assertEquals(300L, currentTime, "coroutineScope must fail and cancel sibling at 300ms without waiting 5,000ms")
        verify(exactly = 0) { assetRepository.save(any()) }
    }

    @Test
    fun `AssetCRUDService deleteAsset is not annotated with imperative Transactional and does not delete DB row if storage delete fails`() = runTest {
        assertNull(
            AssetCRUDService::deleteAsset.findAnnotation<Transactional>(),
            "suspend fun deleteAsset must not carry imperative @Transactional (proven no-op in TransactionalSuspendBehaviorTest)"
        )

        val existingAsset = AssetModel(
            id = 77L,
            bucket = bucket,
            fileName = "map-DEU.svg",
            storageKey = "images/geolocations/49/map-DEU.svg",
            mimeType = "image/svg+xml",
            size = 42L,
            category = AssetCategory.IMAGE,
            status = AssetStatus.AVAILABLE
        )
        every { assetRepository.findById(77L) } returns Optional.of(existingAsset)
        coEvery { storageProvider.delete(bucket.name, existingAsset.storageKey) } throws RuntimeException("MinIO unavailable")

        val crudService = AssetCRUDService(assetRepository, bucketRepository, storageProvider, eventPublisher)

        assertFailsWith<RuntimeException> {
            crudService.deleteAsset(77L)
        }
        verify(exactly = 0) { assetRepository.delete(any()) }
    }
}
