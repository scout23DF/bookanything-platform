package de.org.dexterity.bookanything.dom01geolocation.application.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils

/**
 * Resolves a value from an ordered list of providers (best first), the Chain of Responsibility that
 * the summary and flag services share.
 *
 * Sequential mode (`hedgingEnabled = false`): ask each provider in turn and stop at the first answer.
 * The worst case is the *sum* of every provider's latency.
 *
 * Hedged mode: the leading run of [hedgeable] providers (free, on different hosts) is started with a
 * stagger. The next provider starts when the previous one has not answered within [hedgeDelayMs], or
 * as soon as it answered with nothing. Rules that keep the result predictable:
 * - **Priority wins, not speed.** The result is the answer of the highest-priority provider that has
 *   one, even when a lower-priority provider answered first. Hedging therefore never changes *which*
 *   answer is chosen, only how long we wait for it: the worst case drops from the sum to roughly the
 *   largest [providerBudgetMs].
 * - **Every provider has a budget.** One that exceeds [providerBudgetMs] counts as having no answer.
 * - **Losers are cancelled**, not left running: they are children of the same `coroutineScope`.
 * - **Providers that are not hedgeable** (paid or quota-limited, like Gemini) are only asked
 *   afterwards, one at a time, and only if the hedged group produced nothing.
 */
class ProviderChain(
    private val label: String,
    private val hedgingEnabled: Boolean,
    private val hedgeDelayMs: Long,
    private val providerBudgetMs: Long
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /** The answer of one provider; `value == null` means it had nothing (no result, error, or over budget). */
    private class Attempt<R : Any>(val value: R?)

    suspend fun <P : Any, R : Any> resolve(
        providers: List<P>,
        idOf: (P) -> String,
        hedgeable: (P) -> Boolean,
        call: suspend (P) -> R?
    ): R? {
        val hedgedGroup = if (hedgingEnabled) providers.takeWhile(hedgeable) else emptyList()

        if (hedgedGroup.size > 1) {
            resolveHedged(hedgedGroup, idOf, call)?.let { return it }
        } else if (hedgedGroup.size == 1) {
            attempt(hedgedGroup.first(), idOf, call, budgetMs = providerBudgetMs)?.let { return it }
        }

        for (provider in providers.drop(hedgedGroup.size)) {
            attempt(provider, idOf, call, budgetMs = null)?.let { return it }
        }
        return null
    }

    private suspend fun <P : Any, R : Any> resolveHedged(
        group: List<P>,
        idOf: (P) -> String,
        call: suspend (P) -> R?
    ): R? = coroutineScope {
        val outcomes = group.map { CompletableDeferred<Attempt<R>>() }
        var started = 0

        launch {
            for ((index, provider) in group.withIndex()) {
                launch { outcomes[index].complete(Attempt(attempt(provider, idOf, call, providerBudgetMs))) }
                started = index + 1
                if (index == group.lastIndex) break
                // null = still running after the hedge delay: start the backup. An answer: nobody below can beat it.
                val early = withTimeoutOrNull(hedgeDelayMs) { outcomes[index].await() }
                if (early?.value != null) break
            }
            // Providers that were never started cannot answer; the chooser must not wait for them.
            for (index in started until group.size) outcomes[index].complete(Attempt(null))
        }

        var chosen: R? = null
        for (outcome in outcomes) {
            chosen = outcome.await().value
            if (chosen != null) break
        }
        coroutineContext.cancelChildren() // the losers and the launcher stop here
        chosen
    }

    private suspend fun <P : Any, R : Any> attempt(
        provider: P,
        idOf: (P) -> String,
        call: suspend (P) -> R?,
        budgetMs: Long?
    ): R? {
        val id = idOf(provider)
        val startedAt = System.currentTimeMillis()
        return try {
            val value = if (budgetMs != null) withTimeoutOrNull(budgetMs) { call(provider) } else call(provider)
            val tookMs = System.currentTimeMillis() - startedAt
            when {
                value != null -> logger.info("$label chain: [$id] answered in ${tookMs}ms")
                budgetMs != null && tookMs >= budgetMs -> logger.warn("$label chain: [$id] over its ${budgetMs}ms budget, treated as no answer")
                else -> logger.info("$label chain: [$id] had no answer in ${tookMs}ms")
            }
            value
        } catch (e: CancellationException) {
            logger.debug("$label chain: [$id] cancelled after ${System.currentTimeMillis() - startedAt}ms")
            throw e // never swallow cancellation
        } catch (e: Exception) {
            val rootCause = NestedExceptionUtils.getMostSpecificCause(e)
            logger.warn("$label chain: [$id] failed after ${System.currentTimeMillis() - startedAt}ms: [${rootCause.javaClass.simpleName}] ${rootCause.message}")
            null
        }
    }
}
