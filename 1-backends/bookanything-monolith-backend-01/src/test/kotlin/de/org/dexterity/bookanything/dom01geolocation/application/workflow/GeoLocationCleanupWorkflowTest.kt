package de.org.dexterity.bookanything.dom01geolocation.application.workflow

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupDeletionSummary
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItem
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemPlan
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.CleanupItemResult
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationCleanupRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.NiFiCleanupSummaryDto
import io.temporal.client.WorkflowOptions
import io.temporal.failure.ApplicationFailure
import io.temporal.testing.TestWorkflowEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeCleanupActivities : GeoLocationCleanupActivities {
    val calls = CopyOnWriteArrayList<String>()
    /** Provinces that the plan of a level 0 item reports (as the real plan does). */
    @Volatile var provincesInDb = 2
    @Volatile var failDeleteOf: String? = null
    @Volatile var nifiError: String? = null

    override fun planCleanup(item: CleanupItem): CleanupItemPlan {
        calls += "plan:${item.countrySlug}:${item.locationLevel}"
        val invalid = if (item.countrySlug.length != 3) listOf("countrySlug is not an ISO3 code") else emptyList()
        return CleanupItemPlan(
            countrySlug = item.countrySlug.uppercase(), locationLevel = item.locationLevel,
            targetType = if (item.locationLevel == 0) "COUNTRY" else "PROVINCE",
            geoLocationIds = listOf(1, 2), assetCount = 6, childProvinceCount = provincesInDb, blockers = invalid
        )
    }

    override fun deleteAssetsAndGeoLocations(item: CleanupItem): CleanupDeletionSummary {
        calls += "delete:${item.countrySlug}:${item.locationLevel}"
        if (failDeleteOf == "${item.countrySlug}:${item.locationLevel}") {
            throw ApplicationFailure.newNonRetryableFailure("referenced by addresses", "GEOLOCATION_IN_USE")
        }
        return CleanupDeletionSummary(item.countrySlug, item.locationLevel, 2, 6, 2)
    }

    override fun deleteGeoDataFilesViaNiFi(item: CleanupItem): NiFiCleanupSummaryDto {
        calls += "nifi:${item.countrySlug}:${item.locationLevel}"
        return NiFiCleanupSummaryDto("job", item.countrySlug, item.locationLevel, item.deleteJson, item.deleteXml,
            errorMessage = nifiError)
    }

    override fun verifyCleanup(item: CleanupItem): CleanupItemResult {
        calls += "verify:${item.countrySlug}:${item.locationLevel}"
        return CleanupItemResult(item.countrySlug, item.locationLevel, remainingGeoLocations = 0, remainingAssets = 0)
    }
}

class GeoLocationCleanupWorkflowTest {

    private val queue = "TEST_GEOLOCATION_CLEANUP_TASK_QUEUE"
    private lateinit var env: TestWorkflowEnvironment
    private lateinit var fake: FakeCleanupActivities

    @BeforeEach
    fun setUp() {
        env = TestWorkflowEnvironment.newInstance()
        fake = FakeCleanupActivities()
        val worker = env.newWorker(queue)
        worker.registerWorkflowImplementationTypes(GeoLocationCleanupWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(fake)
        env.start()
    }

    @AfterEach
    fun tearDown() = env.close()

    private fun run(request: GeoLocationCleanupRequest) =
        env.workflowClient.newWorkflowStub(
            GeoLocationCleanupWorkflow::class.java,
            WorkflowOptions.newBuilder().setWorkflowId("geo-cleanup-test").setTaskQueue(queue).build()
        ).executeCleanup(request)

    @Test
    fun `dryRun defaults to true in the request`() {
        assertTrue(GeoLocationCleanupRequest().dryRunEnabled())
        assertTrue(GeoLocationCleanupRequest(items = listOf(CleanupItem("BRA", 1))).dryRunEnabled())
    }

    @Test
    fun `only an explicit false turns the dry run off`() {
        assertTrue(GeoLocationCleanupRequest(dryRun = null).dryRunEnabled(), "null must mean dry run")
        assertTrue(GeoLocationCleanupRequest(dryRun = true).dryRunEnabled())
        assertEquals(false, GeoLocationCleanupRequest(dryRun = false).dryRunEnabled())
    }

    @Test
    fun `a request without dryRun or file flags parses as a dry run that deletes no file`() {
        // The native image cannot use Kotlin defaults of missing properties through Jackson (it failed with
        // "Cannot map null into type boolean"); two mappers cover both ways of building the DTO.
        val body = """{"items":[{"countrySlug":"PRT","locationLevel":1},{"countrySlug":"PRT","locationLevel":0,"shouldDeleteJsonFile":null}]}"""
        listOf(
            com.fasterxml.jackson.databind.ObjectMapper()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES),
            com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        ).forEach { mapper ->
            val request = mapper.readValue(body, GeoLocationCleanupRequest::class.java)
            assertTrue(request.dryRunEnabled(), "a missing dryRun must be a dry run")
            assertEquals(2, request.items.size)
            assertTrue(request.items.none { it.deleteJson || it.deleteXml })
            val explicit = mapper.readValue("""{"items":[],"dryRun":false}""", GeoLocationCleanupRequest::class.java)
            assertEquals(false, explicit.dryRunEnabled())
        }
    }

    @Test
    fun `a dry run returns the plan and deletes nothing`() {
        val result = run(GeoLocationCleanupRequest(listOf(CleanupItem("BRA", 1), CleanupItem("BRA", 0)), dryRun = true))

        assertEquals("DRY_RUN", result.status)
        assertEquals(2, result.plans.size)
        assertTrue(result.results.isEmpty())
        assertTrue(fake.calls.none { it.startsWith("delete:") || it.startsWith("nifi:") })
    }

    @Test
    fun `a country with provinces is blocked unless level 1 is in the same request, even without dryRun`() {
        val result = run(GeoLocationCleanupRequest(listOf(CleanupItem("BRA", 0)), dryRun = false))

        assertEquals("BLOCKED", result.status)
        assertTrue(result.blockers.single().contains("still has 2 province(s)"))
        assertTrue(fake.calls.none { it.startsWith("delete:") })
    }

    @Test
    fun `a country without provinces can be deleted alone`() {
        fake.provincesInDb = 0
        val result = run(GeoLocationCleanupRequest(listOf(CleanupItem("PRT", 0)), dryRun = false))
        assertEquals("COMPLETED", result.status)
        assertEquals(listOf("plan:PRT:0", "delete:PRT:0", "verify:PRT:0"), fake.calls.toList())
    }

    @Test
    fun `a real run deletes the provinces before their country and asks NiFi only when a file flag is set`() {
        val request = GeoLocationCleanupRequest(
            listOf(CleanupItem("BRA", 0, shouldDeleteJsonFile = true), CleanupItem("BRA", 1)), dryRun = false
        )
        val result = run(request)

        assertEquals("COMPLETED", result.status)
        val real = fake.calls.filter { !it.startsWith("plan:") }
        assertEquals(listOf("delete:BRA:1", "verify:BRA:1", "delete:BRA:0", "nifi:BRA:0", "verify:BRA:0"), real)
        assertEquals(listOf(1, 0), result.results.map { it.locationLevel })
        assertTrue(result.results[1].deletedJsonFile)
    }

    @Test
    fun `it stops at the first failure so a country is not deleted with its provinces still there`() {
        fake.failDeleteOf = "BRA:1"
        val result = run(GeoLocationCleanupRequest(listOf(CleanupItem("BRA", 1), CleanupItem("BRA", 0)), dryRun = false))

        assertEquals("COMPLETED_WITH_ERRORS", result.status)
        assertEquals(1, result.results.size)
        assertTrue(result.results[0].errorMessage!!.contains("referenced by addresses"))
        assertTrue(fake.calls.none { it == "delete:BRA:0" })
        assertEquals(1, fake.calls.count { it == "delete:BRA:1" }, "GEOLOCATION_IN_USE is not retried")
    }

    @Test
    fun `an error reported by NiFi is kept in the result`() {
        fake.nifiError = "1 file failed"
        val result = run(GeoLocationCleanupRequest(listOf(CleanupItem("BRA", 1, shouldDeleteXmlFile = true)), dryRun = false))
        assertEquals("COMPLETED_WITH_ERRORS", result.status)
        assertEquals("1 file failed", result.results.single().errorMessage)
    }

    @Test
    fun `an invalid item, a duplicate and an empty request are blocked`() {
        assertEquals("BLOCKED", run(GeoLocationCleanupRequest(listOf(CleanupItem("BR", 1)), dryRun = false)).status)
        val dup = run(GeoLocationCleanupRequest(listOf(CleanupItem("BRA", 1), CleanupItem("bra", 1)), dryRun = false))
        assertEquals("BLOCKED", dup.status)
        assertTrue(dup.blockers.any { it.contains("more than once") })
        assertEquals("BLOCKED", run(GeoLocationCleanupRequest(emptyList())).status)
    }
}
