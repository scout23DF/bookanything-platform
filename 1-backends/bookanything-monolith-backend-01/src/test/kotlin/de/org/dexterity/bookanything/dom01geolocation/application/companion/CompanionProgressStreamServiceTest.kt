package de.org.dexterity.bookanything.dom01geolocation.application.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.ItemState
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.ProgressSnapshot
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowPoll
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowProgressPort
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.WorkflowRunState
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationBatchImportResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ImportProgressDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionProgressStreamServiceTest {

    private val items = listOf(CountryImportItem("DEU", 0), CountryImportItem("LUX", 0))

    private fun running(stage: String, processed: Int, percent: Int = 10) =
        WorkflowPoll(ImportProgressDto(currentStage = stage, percentCompleted = percent, processedItems = processed, totalItems = 2), WorkflowRunState.RUNNING)

    private val completed = WorkflowPoll(ImportProgressDto(currentStage = "COMPLETED", percentCompleted = 100, processedItems = 2, totalItems = 2, status = "COMPLETED"), WorkflowRunState.COMPLETED)

    /** Answers the scripted polls in order (the last one repeats); an [Exception] in the script is thrown. */
    private class ScriptedPort(private val script: List<Any>, private val result: GeoLocationBatchImportResult = GeoLocationBatchImportResult(totalCreated = 7)) : WorkflowProgressPort {
        val polls = AtomicInteger()
        override suspend fun poll(workflowId: String): WorkflowPoll {
            val step = script[minOf(polls.getAndIncrement(), script.lastIndex)]
            if (step is Exception) throw step
            return step as WorkflowPoll
        }
        override suspend fun result(workflowId: String) = result
    }

    private fun service(port: WorkflowProgressPort, pollMs: Long = 1000, failures: Int = 3, maxMs: Long = 60_000) =
        CompanionProgressStreamService(port, pollMs, failures, maxMs)

    @Test
    fun `the state of each item comes from the stage and the processed count`() {
        val s = CompanionProgressStreamService
        assertEquals(listOf(ItemState.INGESTED, ItemState.INGESTING, ItemState.PENDING), (0..2).map { s.stateOf(it, "NIFI_INGESTION", 1) })
        assertEquals(listOf(ItemState.ARTIFACTS_READY, ItemState.GENERATING_ARTIFACTS, ItemState.INGESTED), (0..2).map { s.stateOf(it, "ARTIFACTS_GENERATION", 1) })
        assertEquals(ItemState.COMPLETED, s.stateOf(0, "JSREPORT_GENERATION", 0))
        assertEquals(ItemState.PENDING, s.stateOf(0, "STARTED", 0))
    }

    @Test
    fun `follows the workflow through both passes and ends with the result`() = runTest {
        val port = ScriptedPort(listOf(
            running("NIFI_INGESTION", 0), running("NIFI_INGESTION", 1), running("ARTIFACTS_GENERATION", 0), running("ARTIFACTS_GENERATION", 1), completed
        ))

        val snapshots = service(port).stream("wf", items).toList()

        assertEquals(5, snapshots.size)
        assertEquals(listOf(ItemState.INGESTING, ItemState.PENDING), snapshots[0].items.map { it.state })
        assertEquals(listOf(ItemState.ARTIFACTS_READY, ItemState.GENERATING_ARTIFACTS), snapshots[3].items.map { it.state })
        assertTrue(snapshots.last().terminal)
        assertEquals(7, snapshots.last().result?.totalCreated)
        assertEquals(4000L, testScheduler.currentTime, "one poll per second, none after the end")
    }

    @Test
    fun `an unchanged state is not sent again`() = runTest {
        val port = ScriptedPort(listOf(running("NIFI_INGESTION", 0), running("NIFI_INGESTION", 0), running("NIFI_INGESTION", 0), completed))

        val snapshots = service(port).stream("wf", items).toList()

        assertEquals(2, snapshots.size, "three identical polls produce one snapshot, plus the final one")
        assertEquals(4, port.polls.get())
    }

    @Test
    fun `closing the page stops the polling`() = runTest {
        val port = ScriptedPort(listOf(running("NIFI_INGESTION", 0)))
        val received = mutableListOf<ProgressSnapshot>()
        val collector = launch { service(port).stream("wf", items).collect { received += it } }

        advanceTimeBy(3500)
        runCurrent()
        val pollsBeforeClose = port.polls.get()
        collector.cancel() // the browser closed the tab
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(4, pollsBeforeClose, "polls at 0, 1, 2 and 3 s")
        assertEquals(pollsBeforeClose, port.polls.get(), "no poll after the cancel")
        assertEquals(1, received.size)
    }

    @Test
    fun `a failed workflow ends the stream and marks the unfinished items`() = runTest {
        val port = ScriptedPort(listOf(running("NIFI_INGESTION", 1), WorkflowPoll(ImportProgressDto(currentStage = "NIFI_INGESTION", processedItems = 1, totalItems = 2), WorkflowRunState.FAILED, "Workflow ended as FAILED")))

        val last = service(port).stream("wf", items).toList().last()

        assertTrue(last.terminal)
        assertEquals("Workflow ended as FAILED", last.failure)
        assertEquals(listOf(ItemState.FAILED, ItemState.FAILED), last.items.map { it.state })
    }

    @Test
    fun `a transient poll error is survived`() = runTest {
        val port = ScriptedPort(listOf(running("NIFI_INGESTION", 0), IllegalStateException("temporal blip"), completed))

        val snapshots = service(port).stream("wf", items).toList()

        assertTrue(snapshots.last().terminal)
        assertEquals(null, snapshots.last().failure)
    }

    @Test
    fun `too many errors in a row end the stream instead of hanging`() = runTest {
        val port = ScriptedPort(listOf(IllegalStateException("temporal down")))

        val snapshots = service(port, failures = 3).stream("wf", items).toList()

        assertEquals(1, snapshots.size)
        assertTrue(snapshots.single().terminal)
        assertNotNull(snapshots.single().failure)
        assertEquals(3, port.polls.get())
        assertEquals(2000L, testScheduler.currentTime)
    }

    @Test
    fun `a workflow that never ends is not followed forever`() = runTest {
        val port = ScriptedPort(listOf(running("NIFI_INGESTION", 0)))

        val last = service(port, maxMs = 10_000).stream("wf", items).toList().last()

        assertTrue(last.terminal)
        assertTrue(last.failure!!.contains("Stopped following"))
        assertEquals(10_000L, testScheduler.currentTime)
    }

    @Test
    fun `a slow reader gets the latest picture, not a backlog`() = runTest {
        val script = (0..9).map { running("NIFI_INGESTION", it, percent = it * 10) } + completed
        val port = ScriptedPort(script)
        val received = mutableListOf<ProgressSnapshot>()

        service(port).stream("wf", items).collect {
            received += it
            delay(4500) // slower than the 1 s poll
        }

        assertTrue(received.size < script.size, "older snapshots were dropped: got ${received.size} of ${script.size}")
        assertTrue(received.last().terminal, "but the final one is never lost")
    }
}
