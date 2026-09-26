package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupDeletionSummary
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemPlan
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.NiFiCleanupSummaryDto
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

/** Steps of [GeoLocationCleanupWorkflow] (ADR-0015). All of them are idempotent. */
@ActivityInterface
interface GeoLocationCleanupActivities {

    /** Read only: what a cleanup of the item would delete, and what blocks it. */
    @ActivityMethod
    fun planCleanup(item: CleanupItem): CleanupItemPlan

    /** Deletes assets (tenant objects and rows), corporate report copies and the GeoLocations of the item. */
    @ActivityMethod
    fun deleteAssetsAndGeoLocations(item: CleanupItem): CleanupDeletionSummary

    /** Asks the NiFi cleanup flow to delete the source JSON/XML files (Kafka request/reply). */
    @ActivityMethod
    fun deleteGeoDataFilesViaNiFi(item: CleanupItem): NiFiCleanupSummaryDto

    /** What is left of the item in the database after the cleanup (only the `remaining*` fields are set). */
    @ActivityMethod
    fun verifyCleanup(item: CleanupItem): CleanupItemResult
}
