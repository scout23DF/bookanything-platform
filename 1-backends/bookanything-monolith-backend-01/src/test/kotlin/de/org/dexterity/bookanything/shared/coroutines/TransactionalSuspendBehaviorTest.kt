package de.org.dexterity.bookanything.shared.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.Collections

/**
 * Observes what Spring's imperative `@Transactional` does to a `suspend` function (Spring Boot 4 / Framework 7).
 *
 * The question behind it: `AssetCRUDService.deleteAsset` and `GeoJsonImporterUseCase.execute` are
 * `@Transactional suspend fun`s that cross `withContext(Dispatchers.IO)`. The transaction manager
 * here only records begin/commit/rollback (no database), because the interceptor logic under test
 * does not depend on the concrete [PlatformTransactionManager].
 *
 * Observed (and asserted below): the transaction begins and commits BEFORE the function body runs,
 * and the body executes with no active transaction, even when it never suspends. Spring invokes the
 * body through `CoroutinesUtils.invokeSuspendingFunction`, i.e. as a deferred call, while the
 * interceptor only wraps the creation of that call. So the annotation is silently a no-op for the
 * body's own JPA work (each repository call falls back to its own short transaction).
 */
private val timeline: MutableList<String> = Collections.synchronizedList(mutableListOf())

private fun txActive() = TransactionSynchronizationManager.isActualTransactionActive()

private fun mark(label: String) {
    timeline += "$label tx=${txActive()} thread=${Thread.currentThread().name}"
}

interface TransactionProbe {
    suspend fun suspendsAcrossDispatcher(): String
    suspend fun neverSuspends(): String
}

class TransactionProbeImpl : TransactionProbe {
    @Transactional
    override suspend fun suspendsAcrossDispatcher(): String {
        mark("body-start")
        withContext(Dispatchers.IO) {
            delay(20)
            mark("io-block ")
        }
        mark("body-end  ")
        return "done"
    }

    @Transactional
    override suspend fun neverSuspends(): String {
        mark("body-start")
        mark("body-end  ")
        return "done"
    }
}

class RecordingTransactionManager : AbstractPlatformTransactionManager() {
    override fun doGetTransaction(): Any = Any()
    override fun doBegin(transaction: Any, definition: TransactionDefinition) { timeline += "TX BEGIN" }
    override fun doCommit(status: DefaultTransactionStatus) { timeline += "TX COMMIT" }
    override fun doRollback(status: DefaultTransactionStatus) { timeline += "TX ROLLBACK" }
}

// @TestConfiguration, not @Configuration: Boot's component scan skips it, so the @SpringBootTest integration
// tests (same package tree) do not pick up this probe's transaction manager and CGLIB proxy of a final class.
@TestConfiguration
@EnableTransactionManagement
open class TransactionProbeConfig {
    @Bean
    open fun transactionManager(): PlatformTransactionManager = RecordingTransactionManager()

    @Bean
    open fun transactionProbe(): TransactionProbe = TransactionProbeImpl()
}

class TransactionalSuspendBehaviorTest {

    private fun runProbe(call: suspend (TransactionProbe) -> String): Result<String> {
        timeline.clear()
        AnnotationConfigApplicationContext(TransactionProbeConfig::class.java).use { ctx ->
            val probe = ctx.getBean(TransactionProbe::class.java)
            return runCatching { runBlocking { call(probe) } }
        }
    }

    private fun List<String>.indexOfFirstStartingWith(prefix: String) = indexOfFirst { it.startsWith(prefix) }

    @Test
    fun `transaction is committed before a suspend body that crosses withContext even starts`() {
        val result = runProbe { it.suspendsAcrossDispatcher() }
        println("=== suspendsAcrossDispatcher: result=$result")
        timeline.forEach { println("  $it") }

        assertEquals("done", result.getOrNull())
        val commit = timeline.indexOf("TX COMMIT")
        val bodyStart = timeline.indexOfFirstStartingWith("body-start")
        assertTrue(commit in 0 until bodyStart, "COMMIT must precede the body: $timeline")
        assertTrue(timeline.filter { it.contains("tx=") }.all { it.contains("tx=false") },
            "the body, before and after withContext, must run with no active transaction: $timeline")
        assertTrue(timeline.first { it.startsWith("io-block") }.contains("DefaultDispatcher-worker"),
            "withContext(IO) must have moved the work to a dispatcher worker thread: $timeline")
    }

    @Test
    fun `the same happens to a suspend body that never suspends`() {
        val result = runProbe { it.neverSuspends() }
        println("=== neverSuspends: result=$result")
        timeline.forEach { println("  $it") }

        assertEquals("done", result.getOrNull())
        assertTrue(timeline.indexOf("TX COMMIT") < timeline.indexOfFirstStartingWith("body-start"),
            "COMMIT must precede the body even without a suspension point: $timeline")
        assertTrue(timeline.filter { it.contains("tx=") }.all { it.contains("tx=false") }, timeline.toString())
    }
}
