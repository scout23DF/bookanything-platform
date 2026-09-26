package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemPlan
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationCleanupRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationCleanupResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.failure.ActivityFailure
import io.temporal.workflow.Workflow
import java.time.Duration

class GeoLocationCleanupWorkflowImpl : GeoLocationCleanupWorkflow {

    private fun stub(startToClose: Duration, heartbeat: Duration? = null): GeoLocationCleanupActivities =
        Workflow.newActivityStub(
            GeoLocationCleanupActivities::class.java,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(startToClose)
                .apply { if (heartbeat != null) setHeartbeatTimeout(heartbeat) }
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(3))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofMinutes(1))
                        .setMaximumAttempts(3)
                        // A GeoLocation referenced by other data will not stop being referenced by retrying.
                        .setDoNotRetry("GEOLOCATION_IN_USE")
                        .build()
                )
                .build()
        )

    private val readActivities = stub(Duration.ofMinutes(3))
    private val deleteActivities = stub(Duration.ofMinutes(20))
    private val nifiActivities = stub(Duration.ofMinutes(15), heartbeat = Duration.ofMinutes(1))

    private var progress = ImportProgressDto(currentStage = "PENDING", status = "NOT_STARTED")

    private fun update(stage: String, processed: Int, total: Int, status: String, details: String) {
        progress = ImportProgressDto(
            currentStage = stage,
            percentCompleted = if (total == 0) 0 else (processed * 100) / total,
            processedItems = processed,
            totalItems = total,
            status = status,
            details = details
        )
    }

    override fun executeCleanup(request: GeoLocationCleanupRequest): GeoLocationCleanupResult {
        val jobId = Workflow.getInfo().workflowId
        val items = request.items
        update("PLANNING", 0, items.size, "RUNNING", "Planning the cleanup of ${items.size} item(s)")

        if (items.isEmpty()) {
            update("BLOCKED", 0, 0, "BLOCKED", "No items in the request")
            return GeoLocationCleanupResult(jobId, request.dryRunEnabled(), "BLOCKED", blockers = listOf("The request has no items"))
        }

        // 1. Read-only plan of every item. Nothing is deleted before the whole request is validated.
        val plans = items.map { readActivities.planCleanup(it) }
        val blockers = requestBlockers(items, plans)
        if (blockers.isNotEmpty()) {
            update("BLOCKED", 0, items.size, "BLOCKED", blockers.first())
            return GeoLocationCleanupResult(jobId, request.dryRunEnabled(), "BLOCKED", plans = plans, blockers = blockers)
        }
        if (request.dryRunEnabled()) {
            update("DRY_RUN", items.size, items.size, "DRY_RUN", "Dry run: nothing was deleted")
            return GeoLocationCleanupResult(jobId, true, "DRY_RUN", plans = plans)
        }

        // 2. Real run: provinces (level 1) before their country (level 0).
        val results = mutableListOf<CleanupItemResult>()
        var failed = false
        val ordered = items.sortedByDescending { it.locationLevel }
        for ((index, item) in ordered.withIndex()) {
            update("CLEANING", index, ordered.size, "RUNNING", "Cleaning ${item.countrySlug} level ${item.locationLevel} [${index + 1}/${ordered.size}]")
            val result = cleanItem(item)
            results += result
            if (result.errorMessage != null) {
                // Stop: a country cannot be deleted while its provinces are still there.
                failed = true
                break
            }
        }
        val status = if (failed) "COMPLETED_WITH_ERRORS" else "COMPLETED"
        update(status, results.size, ordered.size, status, if (failed) "Stopped at the first failure" else "Cleanup finished")
        return GeoLocationCleanupResult(jobId, false, status, plans = plans, results = results)
    }

    private fun cleanItem(item: CleanupItem): CleanupItemResult {
        return try {
            val deleted = deleteActivities.deleteAssetsAndGeoLocations(item)
            var deletedJson = false
            var deletedXml = false
            var nifiError: String? = null
            if (item.deleteJson || item.deleteXml) {
                val nifi = nifiActivities.deleteGeoDataFilesViaNiFi(item)
                deletedJson = nifi.deletedJson
                deletedXml = nifi.deletedXml
                nifiError = nifi.errorMessage
            }
            val left = readActivities.verifyCleanup(item)
            CleanupItemResult(
                countrySlug = item.countrySlug.uppercase(),
                locationLevel = item.locationLevel,
                deletedGeoLocations = deleted.deletedGeoLocations,
                deletedAssets = deleted.deletedAssets,
                deletedCorporateReports = deleted.deletedCorporateReports,
                deletedJsonFile = deletedJson,
                deletedXmlFile = deletedXml,
                remainingGeoLocations = left.remainingGeoLocations,
                remainingAssets = left.remainingAssets,
                errorMessage = nifiError
            )
        } catch (e: ActivityFailure) {
            CleanupItemResult(
                countrySlug = item.countrySlug.uppercase(),
                locationLevel = item.locationLevel,
                errorMessage = e.cause?.message ?: e.message ?: "Cleanup step failed"
            )
        }
    }

    /** Rules that span several items, or that one item cannot know about itself. */
    private fun requestBlockers(items: List<CleanupItem>, plans: List<CleanupItemPlan>): List<String> {
        val blockers = plans.flatMap { plan -> plan.blockers.map { "${plan.countrySlug} level ${plan.locationLevel}: $it" } }.toMutableList()
        items.groupBy { it.countrySlug.uppercase() to it.locationLevel }
            .filterValues { it.size > 1 }
            .keys.forEach { (slug, level) -> blockers += "$slug level $level appears more than once in the request" }
        for (plan in plans) {
            if (plan.locationLevel == 0 && plan.childProvinceCount > 0 &&
                items.none { it.countrySlug.equals(plan.countrySlug, ignoreCase = true) && it.locationLevel == 1 }
            ) {
                blockers += "${plan.countrySlug} level 0: the country still has ${plan.childProvinceCount} province(s); " +
                    "include ${plan.countrySlug} level 1 in the request to delete them first"
            }
        }
        return blockers
    }

    override fun getProgress(): ImportProgressDto = progress
}
