package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.ia.gemini

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VertexGeminiIAProxyAdapterTest {

    private class MutableClock(var current: Instant = Instant.parse("2026-10-18T12:00:00Z")) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant(): Instant = current
        fun advance(duration: Duration) {
            current = current.plus(duration)
        }
    }

    private val dummyChatClient = mockk<ChatClient>(relaxed = true)

    @Test
    fun `call exceeding callTimeout returns null at exact virtual timeout and opens circuit after consecutive timeouts`() = runTest {
        val clock = MutableClock()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var backendInvocations = 0

        val adapter = VertexGeminiIAProxyAdapter(
            chatClient = dummyChatClient,
            callTimeout = Duration.ofSeconds(40),
            minCooldown = Duration.ofSeconds(20),
            maxCooldown = Duration.ofMinutes(5),
            maxConcurrentCalls = 2,
            timeoutsToOpen = 2,
            ioDispatcher = testDispatcher,
            clock = clock,
            callExecutor = {
                backendInvocations++
                delay(120_000) // Simulates a hung 120s call in virtual time
                "never-reached"
            }
        )

        // 1st hung call: times out at exactly 40,000ms virtual time
        val firstResult = adapter.simpleSearchByPromptSuspend("prompt-1")
        assertNull(firstResult)
        assertEquals(40_000L, currentTime, "1st call must time out at exactly 40s virtual time")
        assertEquals(1, backendInvocations)

        // 2nd hung call: times out at 80,000ms virtual time and trips circuit breaker (timeoutsToOpen = 2)
        val secondResult = adapter.simpleSearchByPromptSuspend("prompt-2")
        assertNull(secondResult)
        assertEquals(80_000L, currentTime, "2nd call must time out at 80s virtual time")
        assertEquals(2, backendInvocations)

        // 3rd call while circuit is open: fails fast in 0ms virtual time without invoking backend
        val thirdResult = adapter.simpleSearchByPromptSuspend("prompt-3")
        assertNull(thirdResult)
        assertEquals(80_000L, currentTime, "Open circuit must reject in 0ms virtual time")
        assertEquals(2, backendInvocations, "Backend must not be called while circuit is open")

        // Advance clock past the 5-minute maxCooldown -> circuit closes
        clock.advance(Duration.ofMinutes(5).plusSeconds(1))
        val recoveryAdapter = VertexGeminiIAProxyAdapter(
            chatClient = dummyChatClient,
            callTimeout = Duration.ofSeconds(40),
            maxCooldown = Duration.ofMinutes(5),
            ioDispatcher = testDispatcher,
            clock = clock,
            callExecutor = { "recovered" }
        )
        assertEquals("recovered", recoveryAdapter.simpleSearchByPromptSuspend("prompt-4"))
    }

    @Test
    fun `bulkhead rejects extra concurrent callers immediately via Semaphore tryAcquire and releases permits on completion`() = runTest {
        val clock = MutableClock()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val activeCalls = AtomicInteger(0)

        val adapter = VertexGeminiIAProxyAdapter(
            chatClient = dummyChatClient,
            callTimeout = Duration.ofSeconds(40),
            maxConcurrentCalls = 2,
            ioDispatcher = testDispatcher,
            clock = clock,
            callExecutor = { prompt ->
                activeCalls.incrementAndGet()
                try {
                    delay(5_000)
                    "ok:$prompt"
                } finally {
                    activeCalls.decrementAndGet()
                }
            }
        )

        // Launch 2 concurrent calls that occupy both bulkhead permits for 5,000ms virtual time
        val call1 = async { adapter.simpleSearchByPromptSuspend("p1") }
        val call2 = async { adapter.simpleSearchByPromptSuspend("p2") }
        runCurrent()
        assertEquals(2, activeCalls.get())

        // 3rd concurrent call while 2 are in flight must fail fast in 0ms virtual time
        val startVirtual = currentTime
        val rejectedCall3 = adapter.simpleSearchByPromptSuspend("p3")
        assertNull(rejectedCall3, "3rd concurrent call must be rejected by Semaphore bulkhead")
        assertEquals(startVirtual, currentTime, "Bulkhead rejection must take 0ms")

        // Advance virtual time so call1 and call2 finish and release their permits
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals("ok:p1", call1.await())
        assertEquals("ok:p2", call2.await())
        assertEquals(0, activeCalls.get())

        // Now a 4th call succeeds because permits were released in finally
        val call4 = adapter.simpleSearchByPromptSuspend("p4")
        assertEquals("ok:p4", call4)
    }

    @Test
    fun `rate limit 429 parses server retryDelay and daily quota opens circuit for dailyQuotaCooldown`() = runTest {
        val clock = MutableClock()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var invocations = 0
        var failureToThrow: Throwable? = RuntimeException(
            """429 RESOURCE_EXHAUSTED: Quota exceeded. Please retry in 37.5s"""
        )

        val adapter = VertexGeminiIAProxyAdapter(
            chatClient = dummyChatClient,
            callTimeout = Duration.ofSeconds(40),
            minCooldown = Duration.ofSeconds(20),
            maxCooldown = Duration.ofMinutes(5),
            dailyQuotaCooldown = Duration.ofHours(1),
            ioDispatcher = testDispatcher,
            clock = clock,
            callExecutor = {
                invocations++
                failureToThrow?.let { throw it }
                "ok"
            }
        )

        // 1. Trigger 429 with retryDelay 37.5s -> circuit opens for 37.5s
        assertNull(adapter.simpleSearchByPromptSuspend("rate-limited"))
        assertEquals(1, invocations)

        // At +30s, circuit is still open (37.5s required)
        clock.advance(Duration.ofSeconds(30))
        assertNull(adapter.simpleSearchByPromptSuspend("still-open"))
        assertEquals(1, invocations, "Call at +30s must be skipped by circuit breaker")

        // At +38s total, circuit closes and next call executes
        clock.advance(Duration.ofSeconds(8))
        failureToThrow = RuntimeException("429 GenerateRequestsPerDayPerProjectPerModel-FreeTier exhausted")
        assertNull(adapter.simpleSearchByPromptSuspend("daily-quota-exhausted"))
        assertEquals(2, invocations)

        // Daily quota opens circuit for 1 hour: at +30 minutes it is still open
        clock.advance(Duration.ofMinutes(30))
        assertNull(adapter.simpleSearchByPromptSuspend("still-open-daily"))
        assertEquals(2, invocations)

        // At +61 minutes, circuit closes and succeeds
        clock.advance(Duration.ofMinutes(31))
        failureToThrow = null
        assertEquals("ok", adapter.simpleSearchByPromptSuspend("recovered"))
        assertEquals(3, invocations)
    }

    @Test
    fun `external CancellationException is rethrown without tripping circuit breaker and releases bulkhead permit`() = runTest {
        val clock = MutableClock()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var shouldHang = true

        val adapter = VertexGeminiIAProxyAdapter(
            chatClient = dummyChatClient,
            callTimeout = Duration.ofSeconds(40),
            maxConcurrentCalls = 1,
            timeoutsToOpen = 1,
            ioDispatcher = testDispatcher,
            clock = clock,
            callExecutor = {
                if (shouldHang) delay(30_000)
                "ok-after-cancel"
            }
        )

        var caughtCancellation = false
        val job = launch {
            try {
                adapter.simpleSearchByPromptSuspend("cancelled-by-caller")
            } catch (e: CancellationException) {
                caughtCancellation = true
                throw e
            }
        }
        runCurrent()

        // Cancel caller job at 1,000ms (well before the 40s callTimeout)
        advanceTimeBy(1_000)
        job.cancel()
        runCurrent()
        assertTrue(caughtCancellation, "External CancellationException must propagate to caller")

        // Circuit must NOT have opened and the single bulkhead permit must have been released
        shouldHang = false
        assertEquals("ok-after-cancel", adapter.simpleSearchByPromptSuspend("next-call"))
    }

    @Test
    fun `runInterruptible bridges coroutine timeout into Thread interrupt on blocking ChatClient call`() = runTest {
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val wasInterrupted = AtomicBoolean(false)

        val blockingChatClient = mockk<ChatClient>()
        every { blockingChatClient.prompt().user(any<String>()).call().content() } answers {
            started.countDown()
            try {
                Thread.sleep(10_000)
                "unexpected"
            } catch (e: InterruptedException) {
                wasInterrupted.set(true)
                interrupted.countDown()
                throw e
            }
        }

        val adapter = VertexGeminiIAProxyAdapter(
            chatClient = blockingChatClient,
            callTimeout = Duration.ofMillis(80),
            maxConcurrentCalls = 1,
            timeoutsToOpen = 2
        )

        val result = adapter.simpleSearchByPrompt("blocking-sdk-call")
        assertNull(result)
        assertTrue(started.await(1, TimeUnit.SECONDS), "Blocking ChatClient call should have started")
        assertTrue(interrupted.await(2, TimeUnit.SECONDS), "runInterruptible must interrupt the blocked worker thread on timeout")
        assertTrue(wasInterrupted.get())
    }
}
