import asyncio
import logging
from temporalio.client import Client
from temporalio.worker import Worker

from app.core.config import settings
from app.temporal.activities import generate_geo_location_maps_activity

logger = logging.getLogger("temporal_worker")

async def run_temporal_worker():
    """
    Background worker listening for Temporal activities on MAPS_GENERATOR_TASK_QUEUE.
    """
    logger.info("Connecting to Temporal server at: %s (namespace: %s)...", settings.TEMPORAL_HOST_PORT, settings.TEMPORAL_NAMESPACE)
    
    retry_delay = 3
    while True:
        try:
            client = await Client.connect(
                settings.TEMPORAL_HOST_PORT,
                namespace=settings.TEMPORAL_NAMESPACE
            )
            logger.info("Connected to Temporal server successfully!")

            worker = Worker(
                client,
                task_queue=settings.TEMPORAL_TASK_QUEUE,
                activities=[generate_geo_location_maps_activity],
            )

            logger.info("Temporal Worker started and listening on task queue: '%s'", settings.TEMPORAL_TASK_QUEUE)
            await worker.run()
            break
        except asyncio.CancelledError:
            logger.info("Temporal Worker cancelled. Shutting down...")
            break
        except Exception as e:
            logger.warning("Could not connect to Temporal (%s). Retrying in %ds...", e, retry_delay)
            await asyncio.sleep(retry_delay)
            retry_delay = min(retry_delay * 2, 30)
