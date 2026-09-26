package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.FlagStepResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.MapsStepResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.ReportStepResult
import io.temporal.client.WorkflowFailedException
import io.temporal.client.WorkflowOptions
import io.temporal.failure.ApplicationFailure
import io.temporal.testing.TestWorkflowEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Records the calls and lets each test decide which step fails (or blocks). */
private class FakeArtifactsActivities : GeoLocationArtifactsActivities {
    val calls = CopyOnWriteArrayList<String>()
    @Volatile var noGeometry = false
    @Volatile var notFound = false
    @Volatile var copyAlwaysFails = false
    val reportFailuresLeft = AtomicInteger(0)
    @Volatile var mapsGate: CountDownLatch? = null

    override fun generateAndStoreMaps(geoLocationId: Long): MapsStepResult {
        calls += "maps"
        mapsGate?.await(30, TimeUnit.SECONDS)
        if (notFound) throw ApplicationFailure.newNonRetryableFailure("not found", "GEOLOCATION_NOT_FOUND")
        if (noGeometry) {
            return MapsStepResult(geoLocationId, "Nowhere", "PROVINCE", "XX-NW", "SKIPPED_NO_GEOMETRY", "no geometry")
        }
        return MapsStepResult(
            geoLocationId, "Acre", "PROVINCE", "BR-AC",
            localMapAssetId = 10, localMapAssetUrl = "s3://images/map.svg",
            worldHighlightAssetId = 11, worldHighlightAssetUrl = "s3://images/world.svg"
        )
    }

    override fun resolveAndStoreFlag(geoLocationId: Long): FlagStepResult {
        calls += "flag"
        return FlagStepResult(12, "s3://images/flag.svg", "Bandeira de Acre", "descricao", true)
    }

    override fun enrichWithAi(geoLocationId: Long): String {
        calls += "ai"
        return "Acre e um estado do Brasil."
    }

    override fun renderAndStoreReport(
        geoLocationId: Long, maps: MapsStepResult, flag: FlagStepResult, aiSummary: String
    ): ReportStepResult {
        calls += "report"
        if (reportFailuresLeft.getAndDecrement() > 0) throw RuntimeException("JSReport Target closed")
        return ReportStepResult(13, "s3://documents/report.pdf")
    }

    override fun copyReportToCorporateStorage(geoLocationId: Long, reportAssetId: Long): String {
        calls += "copy"
        if (copyAlwaysFails) throw RuntimeException("corporate MinIO down")
        return "s3://darueira-reports/geolocations/report.pdf"
    }

    fun count(step: String) = calls.count { it == step }
}

class GeoLocationArtifactsWorkflowTest {

    private val queue = "TEST_GEOLOCATION_ARTIFACTS_TASK_QUEUE"
    private lateinit var env: TestWorkflowEnvironment
    private lateinit var fake: FakeArtifactsActivities

    @BeforeEach
    fun setUp() {
        env = TestWorkflowEnvironment.newInstance()
        fake = FakeArtifactsActivities()
        val worker = env.newWorker(queue)
        worker.registerWorkflowImplementationTypes(GeoLocationArtifactsWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(fake)
        env.start()
    }

    @AfterEach
    fun tearDown() {
        env.close()
    }

    private fun workflow(id: String = "geo-artifacts-1") =
        env.workflowClient.newWorkflowStub(
            GeoLocationArtifactsWorkflow::class.java,
            WorkflowOptions.newBuilder().setWorkflowId(id).setTaskQueue(queue).build()
        )

    @Test
    fun `runs the steps in order and returns the ids of what each one stored`() {
        val stub = workflow()
        val result = stub.generate(1L)

        assertEquals(listOf("maps", "flag", "ai", "report", "copy"), fake.calls.toList())
        assertEquals("SUCCESS", result.status)
        assertEquals("BR-AC", result.friendlyId)
        assertEquals(10L, result.localMapAssetId)
        assertEquals(11L, result.worldHighlightAssetId)
        assertEquals(12L, result.flagAssetId)
        assertEquals("s3://documents/report.pdf", result.reportPdfUrl)
        assertEquals("Acre e um estado do Brasil.", result.aiEnrichmentSummary)
        assertEquals("DONE", stub.getCurrentStep())
    }

    @Test
    fun `stops after the maps step when the GeoLocation has no geometry`() {
        fake.noGeometry = true
        val result = workflow().generate(1L)

        assertEquals(listOf("maps"), fake.calls.toList())
        assertEquals("SKIPPED_NO_GEOMETRY", result.status)
        assertEquals("XX-NW", result.friendlyId)
    }

    @Test
    fun `a JSReport failure retries only the report step`() {
        fake.reportFailuresLeft.set(2)
        val result = workflow().generate(1L)

        assertEquals("SUCCESS", result.status)
        assertEquals(1, fake.count("maps"))
        assertEquals(1, fake.count("flag"))
        assertEquals(1, fake.count("ai"))
        assertEquals(3, fake.count("report"))
        assertEquals(1, fake.count("copy"))
    }

    @Test
    fun `the item fails when the report step keeps failing`() {
        fake.reportFailuresLeft.set(100)
        assertFailsWith<WorkflowFailedException> { workflow().generate(1L) }
        assertEquals(5, fake.count("report"))
        assertEquals(0, fake.count("copy"))
    }

    @Test
    fun `a failing corporate copy does not fail the item`() {
        fake.copyAlwaysFails = true
        val result = workflow().generate(1L)

        assertEquals("SUCCESS", result.status)
        assertEquals(3, fake.count("copy"))
    }

    @Test
    fun `a GeoLocation that does not exist fails at once without retries`() {
        fake.notFound = true
        assertFailsWith<WorkflowFailedException> { workflow().generate(99L) }
        assertEquals(listOf("maps"), fake.calls.toList())
    }

    // ---- launcher, against the same in-memory Temporal server

    private fun launcher(dedupeSeconds: Long = 300) =
        GeoLocationArtifactsLauncher(env.workflowClient, queue, dedupeSeconds)

    @Test
    fun `the launcher does not start a second run while one is in progress`() {
        val gate = CountDownLatch(1)
        fake.mapsGate = gate
        val launcher = launcher()

        assertTrue(launcher.start(7L), "first start creates the run")
        assertFalse(launcher.start(7L), "a run in progress is not duplicated")
        assertFalse(launcher.start(7L, force = true), "not even when forced: it joins the running one")

        gate.countDown()
        val result = launcher.awaitResult(7L, Duration.ofSeconds(30))
        assertEquals("SUCCESS", result.status)
        assertEquals(1, fake.count("maps"))
    }

    @Test
    fun `the launcher skips a run that just finished unless forced`() {
        val launcher = launcher()
        assertTrue(launcher.start(8L))
        assertEquals("SUCCESS", launcher.awaitResult(8L, Duration.ofSeconds(30)).status)

        assertFalse(launcher.start(8L), "closed within the dedupe window: not started again")
        assertEquals(1, fake.count("maps"))

        assertTrue(launcher.start(8L, force = true), "an explicit request always regenerates")
        assertEquals("SUCCESS", launcher.awaitResult(8L, Duration.ofSeconds(30)).status)
        assertEquals(2, fake.count("maps"))
    }

    @Test
    fun `the launcher starts a new run once the dedupe window has passed`() {
        val launcher = launcher(dedupeSeconds = 0)
        assertTrue(launcher.start(9L))
        launcher.awaitResult(9L, Duration.ofSeconds(30))
        Thread.sleep(1100)

        assertTrue(launcher.start(9L), "window of 0 s: a finished run does not block a new one")
        launcher.awaitResult(9L, Duration.ofSeconds(30))
        assertEquals(2, fake.count("maps"))
    }

    @Test
    fun `the workflow id is geo-artifacts followed by the GeoLocation id and carries a memo`() {
        assertEquals("geo-artifacts-42", GeoLocationArtifactsLauncher.workflowIdFor(42L))
        val launcher = launcher()
        launcher.start(42L)
        launcher.awaitResult(42L, Duration.ofSeconds(30))

        val description = env.workflowClient.newUntypedWorkflowStub("geo-artifacts-42").describe()
        assertNotNull(description.workflowExecutionInfo)
        assertTrue(description.workflowExecutionInfo.hasCloseTime())
    }
}
