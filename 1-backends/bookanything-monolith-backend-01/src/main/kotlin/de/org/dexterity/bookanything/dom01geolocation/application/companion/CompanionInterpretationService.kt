package de.org.dexterity.bookanything.dom01geolocation.application.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.InterpretationResult
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.InterpretationSource
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.RawIntent
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.VoiceCommandInterpreterPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory

/**
 * Turns a transcript into a validated list of imports: the language model first, within a time budget,
 * and the deterministic rules when it is slow, down, out of quota or answers something unusable.
 *
 * The user waits for this call, so the budget is short (a few seconds) even though the Gemini adapter
 * has its own, longer timeout and circuit breaker: when the budget runs out the call is cancelled, not
 * awaited. Cancelling the caller (the HTTP request) cancels the model call too, and is never swallowed.
 */
class CompanionInterpretationService(
    private val primary: VoiceCommandInterpreterPort,
    private val fallback: VoiceCommandInterpreterPort,
    private val validator: ImportIntentValidator,
    private val primaryBudgetMs: Long = 8_000,
    private val maxTranscriptChars: Int = 500
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun interpret(transcript: String): InterpretationResult {
        val text = transcript.trim().take(maxTranscriptChars)
        if (text.isEmpty()) return InterpretationResult(emptyList(), InterpretationSource.NONE, listOf("The command is empty"))

        val fromPrimary = attempt("Gemini", InterpretationSource.GEMINI) { withTimeoutOrNull(primaryBudgetMs) { primary.interpret(text) } }
        if (fromPrimary != null && fromPrimary.items.isNotEmpty()) return fromPrimary

        val fromRules = attempt("rules", InterpretationSource.RULES) { fallback.interpret(text) }
        if (fromRules != null && fromRules.items.isNotEmpty()) {
            val why = if (fromPrimary == null) "the language model did not answer in time" else "the language model found no valid country"
            return fromRules.copy(warnings = fromRules.warnings + "Used the rule-based interpreter because $why")
        }
        return InterpretationResult(emptyList(), InterpretationSource.NONE, (fromRules?.warnings ?: fromPrimary?.warnings ?: emptyList()) + "No country was recognised in the command")
    }

    private suspend fun attempt(label: String, source: InterpretationSource, call: suspend () -> RawIntent?): InterpretationResult? =
        try {
            call()?.let { validator.validate(it, source) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Companion: $label interpreter failed: [${e.javaClass.simpleName}] ${e.message}")
            null
        }
}
