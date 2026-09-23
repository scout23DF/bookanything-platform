package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.ia.gemini

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.SearchEngineInIAProxyPort
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Gemini access with a hard latency budget, so enrichment degrades to the next provider in
 * the chain instead of stalling the Kafka consumer that processes GeoLocations one by one.
 *
 * Previously a single call could take 243-460 s: the google-genai client retries 429s
 * internally with backoff, and this adapter then slept and retried up to 3 more times on
 * top of that, per GeoLocation, per prompt (flag + summary).
 *
 * Now:
 * - Every call gets [callTimeout]; on expiry the caller gets null and moves on.
 * - At most [maxConcurrentCalls] calls in flight; extra callers fail fast instead of queueing
 *   behind hung calls.
 * - Circuit breaker: a rate limit opens it for the server's suggested retry delay (bounded by
 *   [minCooldown]..[maxCooldown]), daily-quota exhaustion for [dailyQuotaCooldown], and
 *   [timeoutsToOpen] consecutive timeouts for [maxCooldown]. While open, calls return null
 *   immediately, so the rest of a batch doesn't pay the same wait again.
 * - Returns null on any failure (never throws): all callers already treat null as "no answer".
 */
class VertexGeminiIAProxyAdapter(
    private val chatClient: ChatClient,
    private val callTimeout: Duration = Duration.ofSeconds(40),
    private val minCooldown: Duration = Duration.ofSeconds(20),
    private val maxCooldown: Duration = Duration.ofMinutes(5),
    private val dailyQuotaCooldown: Duration = Duration.ofHours(1),
    private val maxConcurrentCalls: Int = 2,
    private val timeoutsToOpen: Int = 2
) : SearchEngineInIAProxyPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val executor = ThreadPoolExecutor(
        maxConcurrentCalls, maxConcurrentCalls, 60, TimeUnit.SECONDS, SynchronousQueue()
    ) { runnable -> Thread(runnable, "gemini-call").apply { isDaemon = true } }

    @Volatile
    private var openUntil: Instant = Instant.EPOCH
    private val consecutiveTimeouts = AtomicInteger()

    override fun simpleSearchByPrompt(promptToSearch: String): String? {
        val now = Instant.now()
        if (now.isBefore(openUntil)) {
            logger.info("Gemini circuit open for another ${Duration.between(now, openUntil).seconds}s, skipping call")
            return null
        }

        val startTime = System.currentTimeMillis()
        val future = try {
            CompletableFuture.supplyAsync({
                chatClient.prompt().user(promptToSearch).call().content()
            }, executor)
        } catch (e: RejectedExecutionException) {
            logger.warn("Gemini: $maxConcurrentCalls calls already in flight, skipping call")
            return null
        }

        return try {
            val response = future.get(callTimeout.toMillis(), TimeUnit.MILLISECONDS)
            consecutiveTimeouts.set(0)
            logger.info("Gemini call succeeded in ${System.currentTimeMillis() - startTime}ms (${response?.length ?: 0} chars)")
            response
        } catch (e: TimeoutException) {
            future.cancel(true)
            val timeouts = consecutiveTimeouts.incrementAndGet()
            logger.warn("Gemini call exceeded ${callTimeout.seconds}s budget ($timeouts consecutive)")
            if (timeouts >= timeoutsToOpen) open(maxCooldown, "$timeouts consecutive timeouts")
            null
        } catch (e: ExecutionException) {
            consecutiveTimeouts.set(0)
            handleFailure(e.cause ?: e, System.currentTimeMillis() - startTime)
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            future.cancel(true)
            null
        }
    }

    private fun handleFailure(error: Throwable, durationMs: Long) {
        val causeChain = generateSequence(error) { it.cause }
            .joinToString(" <- ") { "[${it.javaClass.simpleName}: ${it.message}]" }

        val isDailyQuota = causeChain.contains("PerDay", ignoreCase = true) ||
                causeChain.contains("FreeTier", ignoreCase = true)
        val isRateLimit = causeChain.contains("429") || causeChain.contains("RESOURCE_EXHAUSTED") ||
                causeChain.contains("quota", ignoreCase = true) || causeChain.contains("rate limit", ignoreCase = true)

        when {
            isDailyQuota -> open(dailyQuotaCooldown, "daily quota exhausted")
            isRateLimit -> open(
                (retryDelayFrom(causeChain) ?: minCooldown).coerceIn(minCooldown, maxCooldown),
                "rate limited"
            )
            else -> logger.warn("Gemini call failed after ${durationMs}ms: $causeChain")
        }
    }

    /** Gemini's 429 body carries e.g. "retryDelay": "37s" or "Please retry in 37.5s". */
    private fun retryDelayFrom(text: String): Duration? {
        val match = Regex("""retry(?:Delay"?\s*:\s*"?|\s+in\s+)(\d+(?:\.\d+)?)s""", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.let { Duration.ofMillis((it * 1000).toLong()) }
    }

    private fun open(cooldown: Duration, reason: String) {
        openUntil = Instant.now().plus(cooldown)
        logger.warn("Gemini circuit opened for ${cooldown.seconds}s: $reason. Enrichment falls back to other providers meanwhile.")
    }
}
