package de.org.dexterity.bookanything.dom01geolocation.application.companion

import de.org.dexterity.bookanything.dom01geolocation.domain.companion.CountryMention
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.InterpretationSource
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.RawIntent
import de.org.dexterity.bookanything.dom01geolocation.domain.companion.VoiceCommandInterpreterPort
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CountryImportItem
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.companion.RuleBasedVoiceCommandInterpreter
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionInterpretationServiceTest {

    private class Scripted(private val latencyMs: Long, private val answer: () -> RawIntent?) : VoiceCommandInterpreterPort {
        var calls = 0
        override suspend fun interpret(transcript: String): RawIntent? {
            calls++
            delay(latencyMs)
            return answer()
        }
    }

    private fun gemini(latencyMs: Long = 100, answer: () -> RawIntent? = { RawIntent(listOf(CountryMention("Germany", listOf(1)))) }) = Scripted(latencyMs, answer)

    private fun service(primary: VoiceCommandInterpreterPort, budgetMs: Long = 5000) =
        CompanionInterpretationService(primary, RuleBasedVoiceCommandInterpreter(), ImportIntentValidator(), budgetMs)

    @Test
    fun `gemini answers in time - its validated answer is used`() = runTest {
        val r = service(gemini()).interpret("import Germany level 1")

        assertEquals(listOf(CountryImportItem("DEU", 1)), r.items)
        assertEquals(InterpretationSource.GEMINI, r.source)
        assertEquals(100L, testScheduler.currentTime)
    }

    @Test
    fun `gemini too slow - after the budget the rules answer, and the slow call is cancelled`() = runTest {
        var cancelled = false
        val slow = object : VoiceCommandInterpreterPort {
            override suspend fun interpret(transcript: String): RawIntent? =
                try { awaitCancellation() } finally { cancelled = true }
        }

        val r = service(slow, budgetMs = 5000).interpret("import Chile level 2")

        assertEquals(listOf(CountryImportItem("CHL", 2)), r.items)
        assertEquals(InterpretationSource.RULES, r.source)
        assertEquals(5000L, testScheduler.currentTime, "exactly the budget, not the 40 s Gemini timeout")
        assertTrue(cancelled)
        assertTrue(r.warnings.any { it.contains("did not answer in time") })
    }

    @Test
    fun `gemini throws - the rules answer`() = runTest {
        val r = service(gemini { error("quota") }).interpret("import Chile")

        assertEquals(InterpretationSource.RULES, r.source)
        assertEquals(listOf(CountryImportItem("CHL", 0)), r.items)
    }

    @Test
    fun `gemini hallucinates a country - nothing invalid goes through, the rules take over`() = runTest {
        val r = service(gemini { RawIntent(listOf(CountryMention("Atlantis", listOf(0)))) }).interpret("import Peru")

        assertEquals(listOf(CountryImportItem("PER", 0)), r.items)
        assertTrue(r.warnings.any { it.contains("no valid country") })
    }

    @Test
    fun `nobody understands - empty result, nothing to confirm`() = runTest {
        val r = service(gemini { null }).interpret("what is the weather")

        assertTrue(r.items.isEmpty())
        assertEquals(InterpretationSource.NONE, r.source)
    }

    @Test
    fun `blank command never calls gemini`() = runTest {
        val g = gemini()
        val r = service(g).interpret("   ")

        assertEquals(0, g.calls)
        assertTrue(r.items.isEmpty())
    }

    @Test
    fun `cancelling the request cancels the model call and is not turned into a fallback`() = runTest {
        val rulesRan = AtomicBoolean(false)
        val never = object : VoiceCommandInterpreterPort {
            override suspend fun interpret(transcript: String): RawIntent? = awaitCancellation()
        }
        val rules = object : VoiceCommandInterpreterPort {
            override suspend fun interpret(transcript: String): RawIntent? { rulesRan.set(true); return null }
        }
        val job = async(start = CoroutineStart.UNDISPATCHED) {
            CompanionInterpretationService(never, rules, ImportIntentValidator(), 60_000).interpret("import Chile")
        }
        runCurrent()

        job.cancel()
        runCurrent()

        assertTrue(job.isCancelled)
        assertEquals(false, rulesRan.get(), "a cancelled request must not start the fallback")
    }
}
