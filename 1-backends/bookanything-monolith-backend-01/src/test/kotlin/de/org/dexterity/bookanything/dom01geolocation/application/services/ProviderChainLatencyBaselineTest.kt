package de.org.dexterity.bookanything.dom01geolocation.application.services

import com.sun.net.httpserver.HttpServer
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.http.ResilientHttpFetcher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Latency baseline of the chain on the REAL clock and the real [ResilientHttpFetcher], against a local
 * server with fixed latencies. It measures our concurrency, not the internet: the numbers are the shape
 * of the gain (sum vs max), not a production figure.
 *
 * Providers (priority order): A answers 404 after 1200 ms, B answers 200 after 700 ms, C answers 200 after 300 ms.
 * Sequential: A + B = ~1900 ms. Hedged (600 ms stagger): A is over at 1200, B started at 600, so ~1300 ms.
 */
class ProviderChainLatencyBaselineTest {

    private lateinit var server: HttpServer
    private lateinit var fetcher: ResilientHttpFetcher
    private var base = ""

    @BeforeEach
    fun up() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        fun route(path: String, latencyMs: Long, status: Int, body: String) = server.createContext(path) { ex ->
            Thread.sleep(latencyMs)
            val bytes = body.toByteArray()
            ex.sendResponseHeaders(status, if (status == 200) bytes.size.toLong() else -1)
            if (status == 200) ex.responseBody.use { it.write(bytes) } else ex.close()
        }
        route("/a", 1200, 404, "")
        route("/b", 700, 200, "from-B")
        route("/c", 300, 200, "from-C")
        server.start()
        base = "http://127.0.0.1:${server.address.port}"
        fetcher = ResilientHttpFetcher(
            WebClient.create(), timeoutMs = 5000, minIntervalMs = 0, maxRetries = 0,
            maxRetryWaitMs = 1000, cacheEntries = 1, maxConcurrentPerHost = 8
        )
    }

    @AfterEach
    fun down() = server.stop(0)

    private fun measure(hedging: Boolean, runs: Int): List<Long> {
        val chain = ProviderChain("baseline", hedging, hedgeDelayMs = 600, providerBudgetMs = 6000)
        return List(runs) { run ->
            val startedAt = System.nanoTime()
            val answer = runBlocking {
                chain.resolve(listOf("a", "b", "c"), { it }, { true }) { p -> fetcher.getStringSuspend("$base/$p?run=$run&h=$hedging") }
            }
            assertEquals("from-B", answer, "priority B > C in both modes")
            (System.nanoTime() - startedAt) / 1_000_000
        }
    }

    @Test
    fun `baseline - sequential vs hedged on a real clock`() {
        measure(hedging = true, runs = 2) // warm-up: class loading, Netty
        val seq = measure(hedging = false, runs = 10).sorted()
        val hedged = measure(hedging = true, runs = 10).sorted()
        println("BASELINE sequential ms p50=${seq[5]} max=${seq.last()} all=$seq")
        println("BASELINE hedged     ms p50=${hedged[5]} max=${hedged.last()} all=$hedged")
        assertTrue(hedged[5] < seq[5] - 300, "hedging must beat the sum: hedged=${hedged[5]} seq=${seq[5]}")
    }
}
