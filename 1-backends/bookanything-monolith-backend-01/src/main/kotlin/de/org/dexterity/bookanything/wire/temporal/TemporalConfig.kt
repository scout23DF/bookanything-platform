package de.org.dexterity.bookanything.wire.temporal

import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionActivities
import de.org.dexterity.bookanything.dom01geolocation.application.workflow.GeoLocationIngestionWorkflowImpl
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemporalConfig(
    @Value("\${temporal.service-address:10.152.183.189:7233}")
    private val serviceAddress: String,
    @Value("\${temporal.namespace:corporate-core}")
    private val namespace: String,
    @Value("\${temporal.task-queue:GEOLOCATION_INGESTION_TASK_QUEUE}")
    private val taskQueue: String
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

    @Bean
    fun workerFactory(workflowClient: WorkflowClient): WorkerFactory {
        return WorkerFactory.newInstance(workflowClient)
    }

    @Bean
    fun temporalWorkerManager(
        workerFactory: WorkerFactory,
        geoLocationIngestionActivities: GeoLocationIngestionActivities
    ): SmartLifecycle {
        return object : SmartLifecycle {
            private var running = false

            override fun start() {
                try {
                    logger.info("Starting Temporal Worker on Task Queue: '$taskQueue' for namespace: '$namespace'...")
                    val worker = workerFactory.newWorker(taskQueue)
                    worker.registerWorkflowImplementationTypes(GeoLocationIngestionWorkflowImpl::class.java)
                    worker.registerActivitiesImplementations(geoLocationIngestionActivities)

                    workerFactory.start()
                    running = true
                    logger.info("Temporal Worker successfully started and listening on '$taskQueue'!")
                } catch (e: Exception) {
                    logger.warn("Could not start Temporal Worker at $serviceAddress (will retry or continue): ${e.message}")
                }
            }

            override fun stop() {
                logger.info("Stopping Temporal Worker...")
                workerFactory.shutdown()
                running = false
            }

            override fun isRunning(): Boolean = running

            override fun getPhase(): Int = Integer.MAX_VALUE
        }
    }
}
