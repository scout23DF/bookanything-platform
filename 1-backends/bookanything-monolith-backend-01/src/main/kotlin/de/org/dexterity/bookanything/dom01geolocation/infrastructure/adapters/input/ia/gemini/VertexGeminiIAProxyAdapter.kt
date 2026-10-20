package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.ia.gemini

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * Gemini access with a hard latency budget, so enrichment degrades to the next provider in
 * the chain instead of stalling the Kafka consumer that processes GeoLocations one by one.
 *
 * Previously a single call could take 243-460 s: the google-genai client retries 429s
 * internally with backoff, and this adapter then slept and retried up to 3 more times on
 * top of that, per GeoLocation, per prompt (flag + summary).
 *
 * Now implemented with Kotlin Coroutines:
 * - Every call gets [callTimeout] via [withTimeoutOrNull]; on expiry the caller gets null and moves on,
 *   while [runInterruptible] bridges coroutine timeout/cancellation to thread interruption on the blocking SDK call.
 * - At most [maxConcurrentCalls] calls in flight via a non-blocking [Semaphore] (`tryAcquire`) and
 *   `Dispatchers.IO.limitedParallelism(maxConcurrentCalls)`; extra callers fail fast instead of queueing
 *   behind hung calls.
 * - External [CancellationException] from parent scopes is always rethrown to preserve structured concurrency.
 * - Circuit breaker: a rate limit opens it for the server's suggested retry delay (bounded by
 *   [minCooldown]..[maxCooldown]), daily-quota exhaustion for [dailyQuotaCooldown], and
 *   [timeoutsToOpen] consecutive timeouts for [maxCooldown]. While open, calls return null
 *   immediately, so the rest of a batch doesn't pay the same wait again.
 * - Returns null on any non-cancellation failure (never throws): all callers already treat null as "no answer".
 */
class VertexGeminiIAProxyAdapter(
    private val chatClient: ChatClient,
    private val callTimeout: Duration = Duration.ofSeconds(40),
    private val minCooldown: Duration = Duration.ofSeconds(20),
    private val maxCooldown: Duration = Duration.ofMinutes(5),
    private val dailyQuotaCooldown: Duration = Duration.ofHours(1),
    private val maxConcurrentCalls: Int = 2,
    private val timeoutsToOpen: Int = 2,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
    private val callExecutor: (suspend (String) -> String?)? = null
) : SearchEngineInIAProxyPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val bulkhead = Semaphore(maxConcurrentCalls)
    private val geminiDispatcher = ioDispatcher.limitedParallelism(maxConcurrentCalls)

    @Volatile
    private var openUntil: Instant = Instant.EPOCH
    private val consecutiveTimeouts = AtomicInteger()

    private data class CallOutcome(val content: String?)

    override fun simpleSearchByPrompt(promptToSearch: String): String? =
        runBlocking { simpleSearchByPromptSuspend(promptToSearch) }

    override suspend fun simpleSearchByPromptSuspend(promptToSearch: String): String? {
        val now = clock.instant()
        if (now.isBefore(openUntil)) {
            logger.info("Gemini circuit open for another ${Duration.between(now, openUntil).seconds}s, skipping call")
            return null
        }

        if (!bulkhead.tryAcquire()) {
            logger.warn("Gemini: $maxConcurrentCalls calls already in flight, skipping call")
            return null
        }

        val startTime = clock.millis()
        return try {
            val outcome = withTimeoutOrNull(callTimeout.toMillis()) {
                val response = if (callExecutor != null) {
                    withContext(geminiDispatcher) { callExecutor.invoke(promptToSearch) }
                } else {
                    runInterruptible(geminiDispatcher) {
                        chatClient.prompt().user(promptToSearch).call().content()
                    }
                }
                CallOutcome(response)
            }

            if (outcome == null) {
                val timeouts = consecutiveTimeouts.incrementAndGet()
                logger.warn("Gemini call exceeded ${callTimeout.seconds}s budget ($timeouts consecutive)")
                if (timeouts >= timeoutsToOpen) open(maxCooldown, "$timeouts consecutive timeouts")
                null
            } else {
                consecutiveTimeouts.set(0)
                val response = outcome.content
                logger.info("Gemini call succeeded in ${clock.millis() - startTime}ms (${response?.length ?: 0} chars)")
                response
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            consecutiveTimeouts.set(0)
            handleFailure(e, clock.millis() - startTime)
            null
        } finally {
            bulkhead.release()
        }
    }

    private fun handleFailure(error: Throwable, durationMs: Long) {
        val causeChain = generateSequence(error) { it.cause }
            .joinToString(" <- ") { "[${it.javaClass.simpleName}: ${it.message}]" }

        // Only an explicit per-day marker means daily exhaustion. "-FreeTier" alone is not one: it is also
        // the suffix of the per-minute quota ids, so matching it turned every free-tier RPM 429 into 1 h.
        val isDailyQuota = DAILY_QUOTA_MARKER.containsMatchIn(causeChain)
        val isRateLimit = causeChain.contains("429") || causeChain.contains("RESOURCE_EXHAUSTED") ||
                causeChain.contains("quota", ignoreCase = true) || causeChain.contains("rate limit", ignoreCase = true)
        val violated = quotaIdsFrom(causeChain)

        when {
            isDailyQuota -> open(dailyQuotaCooldown, "daily quota exhausted$violated")
            isRateLimit -> open(
                (retryDelayFrom(causeChain) ?: minCooldown).coerceIn(minCooldown, maxCooldown),
                "rate limited$violated"
            )
            else -> logger.warn("Gemini call failed after ${durationMs}ms: $causeChain")
        }
    }

    /** Gemini's 429 body carries e.g. "retryDelay": "37s" or "Please retry in 37.5s". */
    private fun retryDelayFrom(text: String): Duration? {
        val match = Regex("""retry(?:Delay"?\s*:\s*"?|\s+in\s+)(\d+(?:\.\d+)?)s""", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.let { Duration.ofMillis((it * 1000).toLong()) }
    }

    /** The violated quota ids (e.g. GenerateRequestsPerMinutePerProjectPerModel-FreeTier), only to make the log diagnosable. */
    private fun quotaIdsFrom(text: String): String {
        val ids = QUOTA_ID.findAll(text).map { it.value }.distinct().toList()
        return if (ids.isEmpty()) "" else " (violated: ${ids.joinToString()})"
    }

    private fun open(cooldown: Duration, reason: String) {
        openUntil = clock.instant().plus(cooldown)
        logger.warn("Gemini circuit opened for ${cooldown.seconds}s: $reason. Enrichment falls back to other providers meanwhile.")
    }

    private companion object {
        val DAILY_QUOTA_MARKER = Regex("""per[\s_-]?day""", RegexOption.IGNORE_CASE)
        val QUOTA_ID = Regex("""Generate\w*Per(?:Minute|Day|Hour)\w*(?:-\w+)?""")
    }
}
