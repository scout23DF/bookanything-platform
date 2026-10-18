package de.org.dexterity.bookanything.dom01geolocation.application.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behaviour of [ProviderChain] on the virtual time of `runTest`. A fake provider "takes" `latencyMs`
 * and then answers with `answer` (null = no answer); `neverAnswers` hangs until cancelled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProviderChainTest {

    private class FakeProvider(
        val id: String,
        val latencyMs: Long,
        val answer: String?,
        val hedgeable: Boolean = true,
        val neverAnswers: Boolean = false,
        val fails: Boolean = false
    )

    private class Recorder {
        val startedAt = ConcurrentHashMap<String, Long>()
        val cancelled = ConcurrentHashMap.newKeySet<String>()
    }

    private suspend fun ProviderChain.run(
        providers: List<FakeProvider>,
        recorder: Recorder,
        now: () -> Long
    ): String? = resolve(
        providers = providers,
        idOf = { it.id },
        hedgeable = { it.hedgeable },
        call = { p ->
            recorder.startedAt[p.id] = now()
            try {
                when {
                    p.neverAnswers -> awaitCancellation()
                    p.fails -> { delay(p.latencyMs); error("boom from ${p.id}") }
                    else -> { delay(p.latencyMs); p.answer }
                }
            } catch (e: CancellationException) {
                recorder.cancelled += p.id
                throw e
            }
        }
    )

    private fun chain(hedging: Boolean, hedgeDelayMs: Long = 600, budgetMs: Long = 3000) =
        ProviderChain("test", hedging, hedgeDelayMs, budgetMs)

    @Test
    fun `sequential mode pays the sum of the latencies`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 1000, null), FakeProvider("B", 500, "from-B"))

        val result = chain(hedging = false).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-B", result)
        assertEquals(1500L, testScheduler.currentTime)
        assertEquals(1000L, rec.startedAt["B"])
    }

    @Test
    fun `hedging pays the max instead of the sum when the first provider has nothing`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 1000, null), FakeProvider("B", 500, "from-B"))

        val result = chain(hedging = true, hedgeDelayMs = 600).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-B", result)
        assertEquals(600L, rec.startedAt["B"], "B starts after the hedge delay, while A is still running")
        assertEquals(1100L, testScheduler.currentTime, "B answers at 600 + 500; A gave up at 1000")
    }

    @Test
    fun `priority wins over speed even when the lower provider answers first`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 900, "from-A"), FakeProvider("B", 100, "from-B"))

        val result = chain(hedging = true, hedgeDelayMs = 600).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-A", result, "B answered at 700 but A has the higher priority")
        assertEquals(900L, testScheduler.currentTime)
    }

    @Test
    fun `a backup is not even started when the first provider answers within the hedge delay`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 200, "from-A"), FakeProvider("B", 100, "from-B"))

        val result = chain(hedging = true, hedgeDelayMs = 600).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-A", result)
        assertTrue(!rec.startedAt.containsKey("B"), "no wasted call to the second host")
        assertEquals(200L, testScheduler.currentTime)
    }

    @Test
    fun `a provider that answers with nothing starts the next one immediately`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 100, null), FakeProvider("B", 100, "from-B"))

        val result = chain(hedging = true, hedgeDelayMs = 600).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-B", result)
        assertEquals(100L, rec.startedAt["B"], "no need to wait for the hedge delay")
        assertEquals(200L, testScheduler.currentTime)
    }

    @Test
    fun `the loser is cancelled when a higher priority provider wins`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 800, "from-A"), FakeProvider("B", 10_000, "from-B"))

        val result = chain(hedging = true, hedgeDelayMs = 600, budgetMs = 20_000).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-A", result)
        assertEquals(800L, testScheduler.currentTime, "must not wait for the slow loser")
        assertEquals(setOf("B"), rec.cancelled)
    }

    @Test
    fun `a provider over its budget counts as no answer and is cancelled`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 0, null, neverAnswers = true), FakeProvider("B", 100, "from-B"))

        val result = chain(hedging = true, hedgeDelayMs = 600, budgetMs = 3000).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-B", result)
        assertEquals(3000L, testScheduler.currentTime, "B had its answer at 700 but A keeps priority until its 3s budget")
        assertEquals(setOf("A"), rec.cancelled)
    }

    @Test
    fun `a failing provider is just a miss and does not poison the others`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 50, null, fails = true), FakeProvider("B", 100, "from-B"))

        val result = chain(hedging = true).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-B", result)
    }

    @Test
    fun `non-hedgeable providers run only after the hedged group came up empty, one at a time`() = runTest {
        val rec = Recorder()
        val providers = listOf(
            FakeProvider("A", 1000, null),
            FakeProvider("B", 1000, null),
            FakeProvider("PAID", 300, "from-PAID", hedgeable = false)
        )

        val result = chain(hedging = true, hedgeDelayMs = 600).run(providers, rec) { testScheduler.currentTime }

        assertEquals("from-PAID", result)
        assertEquals(1600L, rec.startedAt["PAID"], "after A (0-1000) and B (600-1600) are both done")
    }

    @Test
    fun `the paid provider is never called when the hedged group answers`() = runTest {
        val rec = Recorder()
        val providers = listOf(
            FakeProvider("A", 100, "from-A"),
            FakeProvider("B", 100, "from-B"),
            FakeProvider("PAID", 100, "from-PAID", hedgeable = false)
        )

        assertEquals("from-A", chain(hedging = true).run(providers, rec) { testScheduler.currentTime })

        assertTrue(!rec.startedAt.containsKey("PAID"), "no quota spent")
    }

    @Test
    fun `nobody answers`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 10, null), FakeProvider("B", 10, null))

        assertNull(chain(hedging = true).run(providers, rec) { testScheduler.currentTime })
    }

    @Test
    fun `cancelling the caller cancels every running provider and is not swallowed`() = runTest {
        val rec = Recorder()
        val providers = listOf(
            FakeProvider("A", 0, null, neverAnswers = true),
            FakeProvider("B", 0, null, neverAnswers = true)
        )

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            chain(hedging = true, hedgeDelayMs = 100, budgetMs = 60_000).run(providers, rec) { testScheduler.currentTime }
        }
        testScheduler.advanceTimeBy(150)
        runCurrent()
        assertEquals(setOf("A", "B"), rec.startedAt.keys)

        job.cancel()
        runCurrent()

        assertTrue(job.isCancelled)
        assertEquals(setOf("A", "B"), rec.cancelled)
    }

    @Test
    fun `a single hedgeable provider still honours its budget`() = runTest {
        val rec = Recorder()
        val providers = listOf(FakeProvider("A", 0, null, neverAnswers = true))

        val deferred = async { chain(hedging = true, budgetMs = 3000).run(providers, rec) { testScheduler.currentTime } }

        assertNull(deferred.await())
        assertEquals(3000L, testScheduler.currentTime)
    }
}
