package de.org.dexterity.bookanything.dom01geolocation.domain.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto

enum class WorkflowRunState { RUNNING, COMPLETED, FAILED }

/** One look at a running import: the progress the workflow reports plus whether Temporal says it is still open. */
data class WorkflowPoll(val progress: ImportProgressDto, val state: WorkflowRunState, val failure: String? = null)

/** Reads a workflow's progress. The Temporal client is blocking; implementations keep it off the caller's thread. */
interface WorkflowProgressPort {
    suspend fun poll(workflowId: String): WorkflowPoll
    suspend fun result(workflowId: String): GeoLocationBatchImportResult
}

enum class ItemState { PENDING, INGESTING, INGESTED, GENERATING_ARTIFACTS, ARTIFACTS_READY, COMPLETED, FAILED }

data class ItemProgress(val countrySlug: String, val locationLevel: Int, val state: ItemState)

/** Everything the page needs to draw, as a value: two equal snapshots are the same picture and are not sent twice. */
data class ProgressSnapshot(
    val stage: String,
    val percent: Int,
    val items: List<ItemProgress>,
    val terminal: Boolean = false,
    val failure: String? = null,
    val result: GeoLocationBatchImportResult? = null
)
