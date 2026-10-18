package de.org.dexterity.bookanything.dom01geolocation.application.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.ItemProgress
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.ItemState
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.ProgressSnapshot
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowPoll
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowProgressPort
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowRunState
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory

/**
 * Live progress of an import as a cold [Flow] of [ProgressSnapshot]: it polls the workflow every
 * [pollIntervalMs] and emits only when the picture changed.
 *
 * - **Cancellation is structural.** Nothing here keeps a thread or a timer: the flow is a loop of
 *   `poll` + `delay`, so when the browser closes the tab the collector is cancelled, the `delay` (or
 *   the in-flight poll) is cancelled with it, and Temporal is not asked again.
 * - **A slow client does not slow the poll.** `conflate()` keeps only the newest snapshot for a reader
 *   that cannot keep up; older ones were already out of date.
 * - **Transient failures are tolerated** (up to [maxConsecutiveFailures] polls in a row), then the stream
 *   ends with a failure snapshot instead of hanging. A hard limit of [maxDurationMs] also ends it.
 * - The per-item state is derived from `(currentStage, processedItems)` because the workflow handles the
 *   items in order, in two sequential passes; the workflow itself is not touched.
 */
class CompanionProgressStreamService(
    private val progressPort: WorkflowProgressPort,
    private val pollIntervalMs: Long = 1_000,
    private val maxConsecutiveFailures: Int = 5,
    private val maxDurationMs: Long = 15 * 60_000
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun stream(workflowId: String, items: List<CountryImportItem>): Flow<ProgressSnapshot> =
        flow {
            val finished = withTimeoutOrNull(maxDurationMs) {
                var failures = 0
                while (true) {
                    val poll = try {
                        progressPort.poll(workflowId).also { failures = 0 }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failures++
                        logger.warn("Companion: polling $workflowId failed ($failures/$maxConsecutiveFailures): ${e.message}")
                        if (failures >= maxConsecutiveFailures) {
                            emit(failed(items, "Lost contact with the import workflow: ${e.message}"))
                            return@withTimeoutOrNull
                        }
                        delay(pollIntervalMs)
                        continue
                    }

                    when {
                        poll.state == WorkflowRunState.FAILED -> {
                            emit(failed(items, poll.failure ?: "The import workflow failed", poll))
                            return@withTimeoutOrNull
                        }
                        poll.state == WorkflowRunState.COMPLETED || poll.progress.status == "COMPLETED" -> {
                            val result = try {
                                progressPort.result(workflowId)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                logger.warn("Companion: could not read the result of $workflowId: ${e.message}")
                                null
                            }
                            emit(snapshot(items, poll).copy(terminal = true, result = result))
                            return@withTimeoutOrNull
                        }
                        else -> emit(snapshot(items, poll))
                    }
                    delay(pollIntervalMs)
                }
            }
            if (finished == null) emit(failed(items, "Stopped following the import after ${maxDurationMs / 60_000} minutes (it keeps running in Temporal)"))
        }
            .distinctUntilChanged()
            .conflate()

    private fun failed(items: List<CountryImportItem>, reason: String, poll: WorkflowPoll? = null): ProgressSnapshot {
        val base = poll?.let { snapshot(items, it) }
        val rows = (base?.items ?: items.map { ItemProgress(it.countrySlug, it.locationLevel, ItemState.PENDING) })
            .map { if (it.state == ItemState.COMPLETED) it else it.copy(state = ItemState.FAILED) }
        return ProgressSnapshot(base?.stage ?: "FAILED", base?.percent ?: 0, rows, terminal = true, failure = reason)
    }

    private fun snapshot(items: List<CountryImportItem>, poll: WorkflowPoll): ProgressSnapshot {
        val p = poll.progress
        val rows = items.mapIndexed { index, item -> ItemProgress(item.countrySlug, item.locationLevel, stateOf(index, p.currentStage, p.processedItems)) }
        return ProgressSnapshot(p.currentStage, p.percentCompleted, rows)
    }

    companion object {
        /** State of item [index] given where the workflow is: pass 1 ingests every item, pass 2 builds their artifacts. */
        fun stateOf(index: Int, stage: String, processed: Int): ItemState = when (stage) {
            "NIFI_INGESTION" -> when {
                index < processed -> ItemState.INGESTED
                index == processed -> ItemState.INGESTING
                else -> ItemState.PENDING
            }
            "ARTIFACTS_GENERATION" -> when {
                index < processed -> ItemState.ARTIFACTS_READY
                index == processed -> ItemState.GENERATING_ARTIFACTS
                else -> ItemState.INGESTED
            }
            "JSREPORT_GENERATION", "PUBLISHING_EVENT", "COMPLETED" -> ItemState.COMPLETED
            else -> ItemState.PENDING
        }
    }
}
