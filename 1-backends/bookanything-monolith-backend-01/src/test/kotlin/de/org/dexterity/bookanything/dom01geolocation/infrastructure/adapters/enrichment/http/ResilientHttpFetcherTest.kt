package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.http

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Time-sensitive behaviour of [ResilientHttpFetcher] (pacing, Retry-After, timeout, cancellation,
 * per-host concurrency), checked on the virtual time of `runTest`: no `Thread.sleep`, whole class
 * runs in milliseconds of real time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResilientHttpFetcherTest {

    private fun ok(body: String) = Mono.just(ClientResponse.create(HttpStatus.OK).body(body).build())

    private fun status(code: HttpStatus, retryAfterSeconds: Long? = null) = Mono.just(
        ClientResponse.create(code).apply {
            if (retryAfterSeconds != null) header("Retry-After", retryAfterSeconds.toString())
        }.build()
    )

    /** Records the virtual time at which each request starts and answers through [respond]. */
    private class Exchange(private val scope: TestScope, private val respond: (Int, ClientRequest) -> Mono<ClientResponse>) :
        ExchangeFunction {
        val startedAt = mutableListOf<Long>()
        val calls = AtomicInteger()
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        val cancelled = AtomicInteger()

        override fun exchange(request: ClientRequest): Mono<ClientResponse> = Mono.defer {
            startedAt += scope.testScheduler.currentTime
            val n = calls.incrementAndGet()
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            respond(n, request)
                .doFinally { inFlight.decrementAndGet() }
                .doOnCancel { cancelled.incrementAndGet() }
        }
    }

    private fun TestScope.fetcher(
        exchange: Exchange,
        timeoutMs: Long = 4000,
        minIntervalMs: Long = 250,
        maxRetries: Int = 2,
        maxConcurrentPerHost: Int = 4
    ) = ResilientHttpFetcher(
        WebClient.builder().exchangeFunction(exchange).build(),
        timeoutMs, minIntervalMs, maxRetries, maxRetryWaitMs = 5000, cacheEntries = 100, maxConcurrentPerHost = maxConcurrentPerHost
    ).also { it.nowMs = { testScheduler.currentTime } }

    @Test
    fun `requests to the same host start at least minInterval apart, without blocking a thread`() = runTest {
        val exchange = Exchange(this) { _, _ -> ok("x") }
        val http = fetcher(exchange, minIntervalMs = 250)

        listOf("a", "b", "c").map { async { http.getStringSuspend("https://host.test/$it") } }.awaitAll()

        assertEquals(listOf(0L, 250L, 500L), exchange.startedAt)
        assertEquals(500L, testScheduler.currentTime)
    }

    @Test
    fun `different hosts are paced independently`() = runTest {
        val exchange = Exchange(this) { _, _ -> ok("x") }
        val http = fetcher(exchange, minIntervalMs = 250)

        listOf("https://one.test/a", "https://two.test/a").map { async { http.getStringSuspend(it) } }.awaitAll()

        assertEquals(listOf(0L, 0L), exchange.startedAt)
    }

    @Test
    fun `429 with Retry-After waits that long and then succeeds`() = runTest {
        val exchange = Exchange(this) { n, _ -> if (n == 1) status(HttpStatus.TOO_MANY_REQUESTS, 2) else ok("finally") }
        val http = fetcher(exchange)

        val body = http.getStringSuspend("https://host.test/a")

        assertEquals("finally", body)
        assertEquals(2, exchange.calls.get())
        assertEquals(2000L, exchange.startedAt[1], "second attempt must wait the Retry-After of 2s")
    }

    @Test
    fun `Retry-After is capped at maxRetryWaitMs`() = runTest {
        val exchange = Exchange(this) { n, _ -> if (n == 1) status(HttpStatus.TOO_MANY_REQUESTS, 600) else ok("x") }
        val http = fetcher(exchange)

        http.getStringSuspend("https://host.test/a")

        assertEquals(5000L, exchange.startedAt[1])
    }

    @Test
    fun `404 is negatively cached and does not hit the network again`() = runTest {
        val exchange = Exchange(this) { _, _ -> status(HttpStatus.NOT_FOUND) }
        val http = fetcher(exchange)

        assertNull(http.getBytesSuspend("https://host.test/missing"))
        assertNull(http.getBytesSuspend("https://host.test/missing"))

        assertEquals(1, exchange.calls.get())
    }

    @Test
    fun `successful responses are cached`() = runTest {
        val exchange = Exchange(this) { _, _ -> ok("payload") }
        val http = fetcher(exchange)

        assertContentEquals("payload".toByteArray(), http.getBytesSuspend("https://host.test/a"))
        assertContentEquals("payload".toByteArray(), http.getBytesSuspend("https://host.test/a"))

        assertEquals(1, exchange.calls.get())
    }

    @Test
    fun `a request that never answers times out, is retried once and then gives up`() = runTest {
        val exchange = Exchange(this) { _, _ -> Mono.never() }
        val http = fetcher(exchange, timeoutMs = 4000, minIntervalMs = 0)

        val result = http.getBytesSuspend("https://host.test/slow")

        assertNull(result)
        assertEquals(2, exchange.calls.get(), "one attempt plus one retry")
        assertEquals(8000L, testScheduler.currentTime, "two timeouts of 4s, in virtual time")
        assertEquals(2, exchange.cancelled.get(), "each timed out call must be cancelled, not left running")
    }

    @Test
    fun `cancelling the caller cancels the in-flight HTTP call and is not swallowed`() = runTest {
        val exchange = Exchange(this) { _, _ -> Mono.never() }
        val http = fetcher(exchange, minIntervalMs = 0)

        val job = launch(start = CoroutineStart.UNDISPATCHED) { http.getBytesSuspend("https://host.test/slow") }
        runCurrent()
        assertEquals(1, exchange.inFlight.get())

        job.cancel()
        runCurrent()

        assertTrue(job.isCancelled, "the CancellationException must propagate to the caller")
        assertEquals(1, exchange.cancelled.get(), "the reactor subscription must have been cancelled")
        assertEquals(0, exchange.inFlight.get())
        assertEquals(1, exchange.calls.get(), "no retry after a cancellation")
    }

    @Test
    fun `a timeout imposed by the caller is not treated as one of our retryable timeouts`() = runTest {
        val exchange = Exchange(this) { _, _ -> Mono.never() }
        val http = fetcher(exchange, timeoutMs = 4000, minIntervalMs = 0)

        val outcome = runCatching { kotlinx.coroutines.withTimeout(1000) { http.getBytesSuspend("https://host.test/slow") } }

        assertTrue(outcome.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException)
        assertEquals(1, exchange.calls.get(), "the caller's deadline must stop the retry loop")
        assertEquals(1000L, testScheduler.currentTime)
    }

    @Test
    fun `at most maxConcurrentPerHost requests are in flight to one host`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val exchange = Exchange(this) { _, _ ->
            mono(dispatcher) { delay(1000); ClientResponse.create(HttpStatus.OK).body("x").build() }
        }
        val http = fetcher(exchange, minIntervalMs = 0, maxConcurrentPerHost = 2)

        (1..6).map { async { http.getStringSuspend("https://host.test/$it") } }.awaitAll()

        assertEquals(2, exchange.maxInFlight.get())
        assertEquals(3000L, testScheduler.currentTime, "6 calls of 1s through 2 permits take 3s")
    }

    @Test
    fun `advancing virtual time past the pacing slot releases a waiting request`() = runTest {
        val exchange = Exchange(this) { _, _ -> ok("x") }
        val http = fetcher(exchange, minIntervalMs = 250)

        http.getStringSuspend("https://host.test/a")
        val second = async { http.getStringSuspend("https://host.test/b") }
        runCurrent()
        assertEquals(1, exchange.calls.get(), "second request is parked in delay(), no thread is blocked")

        advanceTimeBy(250)
        runCurrent()
        assertEquals(2, exchange.calls.get())
        second.await()
    }
}
