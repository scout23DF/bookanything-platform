package de.org.dexterity.bookanything.dom01geolocation.application.services

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupDeletionSummary
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemPlan
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.CorporateStoragePort
import de.org.dexterity.bookanything.dom02assetmanager.application.services.AssetCRUDService
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import de.org.dexterity.bookanything.dom02assetmanager.domain.ports.AssetPersistRepositoryPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Plans, executes and verifies the cleanup of the GeoLocations of a country/level (ADR-0015).
 *
 * Only COUNTRY (level 0) and PROVINCE (level 1) are ever touched: CONTINENT and REGION are reference data and
 * cannot be reached through here (the target type is derived from the level, never from the caller).
 * Everything is idempotent: the targets are read again on every call, so a retry continues where it stopped.
 */
@Service
class GeoLocationCleanupService(
    private val geoLocationCRUDService: GeoLocationCRUDService,
    private val assetRepository: AssetPersistRepositoryPort,
    private val assetCRUDService: AssetCRUDService,
    private val corporateStorage: CorporateStoragePort,
    transactionManager: PlatformTransactionManager,
    @Value("\${corporate.minio.geodata-bucket:darueira-geodata}")
    private val geodataBucket: String,
    @Value("\${corporate.minio.reports-bucket:darueira-reports}")
    private val reportsBucket: String,
    @Value("\${application.domain-settings.geolocation.cleanup.max-concurrent-asset-deletes:4}")
    private val maxConcurrentAssetDeletes: Int = 4
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // The JPA mappers read lazy collections: reads happen in short transactions (see ADR-0014).
    private val readTx = TransactionTemplate(transactionManager).apply { isReadOnly = true }
    private val writeTx = TransactionTemplate(transactionManager)

    companion object {
        private val SLUG = Regex("^[A-Za-z]{3}$")
        const val TENANT_IMAGES_PREFIX = "images/geolocations/"
        const val TENANT_DOCUMENTS_PREFIX = "documents/geolocations/"

        fun geodataJsonKey(slug: String, level: Int) = "raw/gadm41_${slug}_$level.json"
        fun geodataXmlKey(slug: String, level: Int) = "xml/gadm41_${slug}_$level.xml"

        /** Same key the artifacts workflow uses for the corporate copy of the detail report. */
        fun corporateReportKey(geoLocation: IGeoLocationModel): String {
            val idOrAlias = (geoLocation.alias ?: geoLocation.friendlyId).replace("/", "-")
            return "geolocations/geolocation-detail-report-$idOrAlias.pdf"
        }
    }

    /** Problems with the item itself, before touching the database. Empty when the item is acceptable. */
    fun validate(item: CleanupItem): List<String> {
        val problems = mutableListOf<String>()
        if (!SLUG.matches(item.countrySlug)) {
            problems += "countrySlug '${item.countrySlug}' is not an ISO3 code (3 letters)"
        }
        if (item.locationLevel != 0 && item.locationLevel != 1) {
            problems += "locationLevel ${item.locationLevel} is not supported: only 0 (country) and 1 (provinces)"
        }
        return problems
    }

    private fun targetType(level: Int) = if (level == 0) GeoLocationType.COUNTRY else GeoLocationType.PROVINCE

    private fun findCountry(slug: String): IGeoLocationModel? =
        readTx.execute {
            geoLocationCRUDService.findByFriendlyId(GeoLocationType.COUNTRY, slug, PageRequest.of(0, 20))
                .content.firstOrNull { it.friendlyId.equals(slug, ignoreCase = true) }
        }

    private fun provincesOf(countryId: Long): List<IGeoLocationModel> =
        readTx.execute {
            val all = mutableListOf<IGeoLocationModel>()
            var page = 0
            do {
                val result = geoLocationCRUDService.searchByParentIdAndNameStartingWith(
                    GeoLocationType.PROVINCE, countryId, "", PageRequest.of(page, 200)
                )
                all += result.content
                page++
            } while (result.hasNext())
            all
        } ?: emptyList()

    /** The GeoLocations that a cleanup of this item deletes: the country (level 0) or its provinces (level 1). */
    private fun targets(item: CleanupItem): List<IGeoLocationModel> {
        val country = findCountry(item.countrySlug) ?: return emptyList()
        return if (item.locationLevel == 0) listOf(country) else provincesOf(country.id.id)
    }

    private fun assetsOf(geoLocationId: Long): List<AssetModel> =
        listOf("$TENANT_IMAGES_PREFIX$geoLocationId/", "$TENANT_DOCUMENTS_PREFIX$geoLocationId/").flatMap { prefix ->
            val found = mutableListOf<AssetModel>()
            var page = 0
            do {
                val result = assetRepository.findByStorageKeyStartingWith(prefix, PageRequest.of(page, 200))
                found += result.content
                page++
            } while (result.hasNext())
            found
        }

    /** Read-only: what a cleanup of [item] would delete. */
    fun plan(item: CleanupItem): CleanupItemPlan {
        val slug = item.countrySlug.uppercase()
        val problems = validate(item)
        if (problems.isNotEmpty()) {
            return CleanupItemPlan(countrySlug = item.countrySlug, locationLevel = item.locationLevel, blockers = problems)
        }
        val country = findCountry(slug)
        val targets = targets(item.copy(countrySlug = slug))
        val provinceCount = country?.let { provincesOf(it.id.id).size } ?: 0
        val assetCount = targets.sumOf { assetsOf(it.id.id).size }
        val reports = targets.count { corporateStorage.exists(reportsBucket, corporateReportKey(it)) }
        return CleanupItemPlan(
            countrySlug = slug,
            locationLevel = item.locationLevel,
            targetType = targetType(item.locationLevel).name,
            geoLocationIds = targets.map { it.id.id },
            assetCount = assetCount,
            corporateReportCount = reports,
            childProvinceCount = provinceCount,
            jsonFileExists = corporateStorage.exists(geodataBucket, geodataJsonKey(slug, item.locationLevel)),
            xmlFileExists = corporateStorage.exists(geodataBucket, geodataXmlKey(slug, item.locationLevel)),
            willDeleteJsonFile = item.deleteJson,
            willDeleteXmlFile = item.deleteXml
        )
    }

    /**
     * Deletes the assets of a GeoLocation concurrently with bounded parallelism ([Semaphore])
     * instead of calling `runBlocking` sequentially inside a `forEach` loop.
     */
    internal suspend fun deleteAssetsConcurrently(targetAssets: List<AssetModel>): Int {
        if (targetAssets.isEmpty()) return 0
        val semaphore = Semaphore(maxConcurrentAssetDeletes.coerceAtLeast(1))
        return coroutineScope {
            targetAssets.map { asset ->
                async {
                    semaphore.withPermit {
                        assetCRUDService.deleteAsset(asset.id!!)
                    }
                }
            }.awaitAll().count { it }
        }
    }

    /**
     * Deletes the assets (tenant objects and rows), the corporate report copies and the GeoLocations of the item.
     * A GeoLocation referenced by other data (addresses, cities) makes the deletion fail: the caller gets the
     * exception and nothing else is deleted for that GeoLocation.
     */
    fun deleteData(item: CleanupItem): CleanupDeletionSummary {
        val slug = item.countrySlug.uppercase()
        require(validate(item).isEmpty()) { "Invalid cleanup item: ${validate(item)}" }
        var geoLocations = 0
        var assets = 0
        var reports = 0
        for (target in targets(item.copy(countrySlug = slug))) {
            val id = target.id.id
            val targetAssets = assetsOf(id)
            if (targetAssets.isNotEmpty()) {
                assets += runBlocking(Dispatchers.IO) { deleteAssetsConcurrently(targetAssets) }
            }
            val reportKey = corporateReportKey(target)
            if (corporateStorage.exists(reportsBucket, reportKey)) {
                corporateStorage.delete(reportsBucket, reportKey)
                reports++
            }
            writeTx.execute { geoLocationCRUDService.deleteById(targetType(item.locationLevel), id) }
            geoLocations++
            logger.info("Cleanup: deleted GeoLocation #$id (${target.friendlyId}) of $slug level ${item.locationLevel}")
        }
        return CleanupDeletionSummary(
            countrySlug = slug, locationLevel = item.locationLevel,
            deletedGeoLocations = geoLocations, deletedAssets = assets, deletedCorporateReports = reports
        )
    }

    /** What is left of the item in the database (0 and 0 when the cleanup worked). */
    fun remaining(item: CleanupItem): Pair<Int, Int> {
        val targets = targets(item.copy(countrySlug = item.countrySlug.uppercase()))
        return Pair(targets.size, targets.sumOf { assetsOf(it.id.id).size })
    }
}
