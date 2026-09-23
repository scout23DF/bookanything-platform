package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.enrichment.http

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.URI
import java.time.Duration
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared HTTP GET client for enrichment providers (Wikidata, Wikipedia, Wikimedia Commons,
 * flagcdn, restcountries).
 *
 * Replaces each provider's own `webClient.get()...block()` + `catch { null }`, which:
 * - swallowed every failure at DEBUG level (a 404 from a wrong URL and a 429 from rate
 *   limiting looked identical: "returned null/empty"),
 * - hammered upload.wikimedia.org with no pacing, which answers 429 quickly,
 * - re-downloaded the same resources on every import.
 *
 * Behaviour:
 * - Per-host pacing: at most one request per [minIntervalMs] to the same host.
 * - 429/503: honours Retry-After (capped at [maxRetryWaitMs]), up to [maxRetries] retries.
 *   Other 5xx and network errors: one quick retry. 4xx (404 etc.): no retry.
 * - Successful responses up to [maxCacheableBytes] are cached (bounded LRU), as are 404s
 *   (negative cache), so re-imports and repeated lookups cost nothing.
 * - Every non-success is logged once at WARN (404 at DEBUG) with the HTTP status.
 */
@Component
class ResilientHttpFetcher(
    private val webClient: WebClient,
    @Value("\${application.domain-settings.geolocation.enrichment.http.timeout-ms:4000}")
    private val timeoutMs: Long,
    @Value("\${application.domain-settings.geolocation.enrichment.http.min-interval-per-host-ms:250}")
    private val minIntervalMs: Long,
    @Value("\${application.domain-settings.geolocation.enrichment.http.max-retries:2}")
    private val maxRetries: Int,
    @Value("\${application.domain-settings.geolocation.enrichment.http.max-retry-wait-ms:5000}")
    private val maxRetryWaitMs: Long,
    @Value("\${application.domain-settings.geolocation.enrichment.http.cache-entries:500}")
    private val cacheEntries: Int
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val maxCacheableBytes = 256 * 1024 // bounds worst-case cache heap to ~cacheEntries * 256 KB

    private val nextSlotByHost = ConcurrentHashMap<String, Long>()
    private val cache: MutableMap<String, CachedResponse> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedResponse>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedResponse>?) = size > cacheEntries
        }
    )

    private data class CachedResponse(val body: ByteArray?)

    fun getBytes(url: String, accept: String? = null): ByteArray? {
        cache[url]?.let { return it.body }

        val uri = URI.create(url)
        val host = uri.host ?: "unknown"
        var attempt = 0
        while (true) {
            awaitHostSlot(host)
            try {
                val bytes = webClient.get()
                    .uri(uri)
                    .headers { h -> if (accept != null) h.set("Accept", accept) }
                    .retrieve()
                    .bodyToMono(ByteArray::class.java)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block()
                if (bytes != null && bytes.size <= maxCacheableBytes) cache[url] = CachedResponse(bytes)
                return bytes
            } catch (e: WebClientResponseException) {
                val status = e.statusCode.value()
                if (status == 404 || status == 410) {
                    logger.debug("HTTP $status for $url")
                    cache[url] = CachedResponse(null)
                    return null
                }
                val retryable = status == 429 || status >= 500
                if (!retryable || attempt >= maxRetries) {
                    logger.warn("HTTP $status for $url (attempt ${attempt + 1}), giving up")
                    return null
                }
                val waitMs = retryAfterMs(e) ?: (500L shl attempt)
                logger.warn("HTTP $status for $url (attempt ${attempt + 1}), retrying in ${waitMs}ms")
                pushHostSlot(host, waitMs)
            } catch (e: Exception) {
                if (attempt >= 1) {
                    logger.warn("Request to $url failed: [${e.javaClass.simpleName}] ${e.message}")
                    return null
                }
                logger.debug("Request to $url failed, retrying once: [${e.javaClass.simpleName}] ${e.message}")
            }
            attempt++
        }
    }

    fun getString(url: String, accept: String? = null): String? =
        getBytes(url, accept)?.toString(Charsets.UTF_8)

    private fun retryAfterMs(e: WebClientResponseException): Long? {
        val header = e.headers.getFirst("Retry-After") ?: return null
        val seconds = header.trim().toLongOrNull() ?: return null
        return (seconds * 1000).coerceIn(0, maxRetryWaitMs)
    }

    /** Blocks until this host's next request slot, then reserves the following one. */
    private fun awaitHostSlot(host: String) {
        var waitMs = 0L
        nextSlotByHost.compute(host) { _, next ->
            val now = System.currentTimeMillis()
            val slot = maxOf(now, next ?: now)
            waitMs = slot - now
            slot + minIntervalMs
        }
        if (waitMs > 0) Thread.sleep(waitMs)
    }

    private fun pushHostSlot(host: String, delayMs: Long) {
        nextSlotByHost.merge(host, System.currentTimeMillis() + delayMs) { a, b -> maxOf(a, b) }
    }
}
