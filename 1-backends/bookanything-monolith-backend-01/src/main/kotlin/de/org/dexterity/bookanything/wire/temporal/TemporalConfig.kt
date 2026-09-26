package de.org.dexterity.bookanything.wire.temporal

import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationArtifactsActivities
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationArtifactsWorkflowImpl
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationCleanupActivities
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationCleanupWorkflowImpl
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionActivities
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionWorkflowImpl
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import io.temporal.worker.WorkerOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Configuration
class TemporalConfig(
    @Value("\${temporal.service-address:10.152.183.189:7233}")
    private val serviceAddress: String,
    @Value("\${temporal.namespace:corporate-core}")
    private val namespace: String,
    @Value("\${temporal.task-queue:GEOLOCATION_INGESTION_TASK_QUEUE}")
    private val taskQueue: String,
    @Value("\${temporal.artifacts.task-queue:GEOLOCATION_ARTIFACTS_TASK_QUEUE}")
    private val artifactsTaskQueue: String,
    // Window of concurrent artifact steps per worker (ADR-0014): the GeoPandas subprocess and JSReport are
    // heavy, and NiFi keeps posting GeoLocations to the same Pod meanwhile.
    @Value("\${temporal.artifacts.max-concurrent-activities:4}")
    private val artifactsMaxConcurrentActivities: Int,
    @Value("\${temporal.worker-start.initial-backoff-seconds:5}")
    private val initialBackoffSeconds: Long,
    @Value("\${temporal.worker-start.max-backoff-seconds:60}")
    private val maxBackoffSeconds: Long
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun workflowServiceStubs(): WorkflowServiceStubs {
        logger.info("Initializing Temporal WorkflowServiceStubs targeting: $serviceAddress")
        val options = WorkflowServiceStubsOptions.newBuilder()
            .setTarget(serviceAddress)
            .build()
        return WorkflowServiceStubs.newServiceStubs(options)
    }

    @Bean
    fun workflowClient(workflowServiceStubs: WorkflowServiceStubs): WorkflowClient {
        logger.info("Initializing Temporal WorkflowClient for namespace: $namespace")
        val options = WorkflowClientOptions.newBuilder()
            .setNamespace(namespace)
            .build()
        return WorkflowClient.newInstance(workflowServiceStubs, options)
    }

    /**
     * Starts the Temporal worker, retrying in the background until the server is reachable.
     *
     * Previously a single failed attempt (e.g. Temporal still booting after a node restart)
     * only logged a warning and the app ran on with no worker polling the task queue, so
     * workflows were accepted but never executed ("No Workers Running" in the Temporal UI).
     *
     * Each attempt builds a fresh WorkerFactory. Retrying start() on the same factory is NOT
     * safe in SDK 1.30.x: after a failed start (server UNAVAILABLE) the factory is left marked
     * as started, so the next start() returns immediately without launching any pollers. The
     * app then logs "successfully started" while the task queue has no pollers at all.
     */
    @Bean
    fun temporalWorkerManager(
        workflowClient: WorkflowClient,
        geoLocationIngestionActivities: GeoLocationIngestionActivities,
        geoLocationArtifactsActivities: GeoLocationArtifactsActivities,
        geoLocationCleanupActivities: GeoLocationCleanupActivities
    ): SmartLifecycle {
        return object : SmartLifecycle {
            @Volatile
            private var running = false
            @Volatile
            private var workerFactory: WorkerFactory? = null
            private var retryExecutor: ScheduledExecutorService? = null

            override fun start() {
                retryExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
                    Thread(runnable, "temporal-worker-starter").apply { isDaemon = true }
                }
                running = true
                scheduleStartAttempt(attempt = 1, delaySeconds = 0)
            }

            private fun scheduleStartAttempt(attempt: Int, delaySeconds: Long) {
                retryExecutor?.schedule({ tryStart(attempt, delaySeconds) }, delaySeconds, TimeUnit.SECONDS)
            }

            private fun newWorkerFactory(): WorkerFactory {
                val factory = WorkerFactory.newInstance(workflowClient)
                val worker = factory.newWorker(taskQueue)
                worker.registerWorkflowImplementationTypes(
                    GeoLocationIngestionWorkflowImpl::class.java,
                    GeoLocationCleanupWorkflowImpl::class.java
                )
                worker.registerActivitiesImplementations(geoLocationIngestionActivities, geoLocationCleanupActivities)

                // Per-GeoLocation artifacts: own queue, and at most N steps executing at the same time.
                val artifactsWorker = factory.newWorker(
                    artifactsTaskQueue,
                    WorkerOptions.newBuilder()
                        .setMaxConcurrentActivityExecutionSize(artifactsMaxConcurrentActivities)
                        .build()
                )
                artifactsWorker.registerWorkflowImplementationTypes(GeoLocationArtifactsWorkflowImpl::class.java)
                artifactsWorker.registerActivitiesImplementations(geoLocationArtifactsActivities)
                return factory
            }

            private fun tryStart(attempt: Int, previousDelaySeconds: Long) {
                if (!running) return
                val factory = newWorkerFactory()
                try {
                    logger.info("Starting Temporal Worker on Task Queue: '$taskQueue' for namespace: '$namespace' (attempt $attempt)...")
                    factory.start()
                    workerFactory = factory
                    logger.info("Temporal Worker successfully started and listening on '$taskQueue'!")
                    retryExecutor?.shutdown()
                } catch (e: Exception) {
                    factory.shutdownNow()
                    val nextDelay = if (previousDelaySeconds == 0L) initialBackoffSeconds
                                    else minOf(previousDelaySeconds * 2, maxBackoffSeconds)
                    logger.warn("Could not start Temporal Worker at $serviceAddress (attempt $attempt): ${e.message}. Retrying in ${nextDelay}s")
                    scheduleStartAttempt(attempt + 1, nextDelay)
                }
            }

            override fun stop() {
                logger.info("Stopping Temporal Worker...")
                running = false
                retryExecutor?.shutdownNow()
                workerFactory?.shutdown()
            }

            override fun isRunning(): Boolean = running

            override fun getPhase(): Int = Integer.MAX_VALUE
        }
    }
}
