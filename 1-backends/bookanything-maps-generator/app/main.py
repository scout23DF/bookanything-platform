import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.api.v1.endpoints.maps import router as maps_router
from app.temporal.worker import run_temporal_worker

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(name)s] %(message)s"
)
logger = logging.getLogger("main")

temporal_task = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global temporal_task
    logger.info("Starting %s v%s...", settings.APP_NAME, settings.APP_VERSION)
    
    if settings.TEMPORAL_WORKER_ENABLED:
        logger.info("Spawning Temporal Worker background task on queue '%s'...", settings.TEMPORAL_TASK_QUEUE)
        temporal_task = asyncio.create_task(run_temporal_worker())
    else:
        logger.info("Temporal Worker is disabled by configuration.")
        
    yield
    
    if temporal_task:
        logger.info("Cancelling Temporal Worker background task...")
        temporal_task.cancel()
        try:
            await temporal_task
        except asyncio.CancelledError:
            pass
    logger.info("Application shutdown complete.")

app = FastAPI(
    title="BookAnything Maps Generator API",
    description="Microservice for generating high-fidelity cartographic SVG maps using GeoPandas, Shapely, and Matplotlib with Temporal polyglot activity support.",
    version=settings.APP_VERSION,
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(maps_router)

@app.get("/healthz")
@app.get("/health")
async def health():
    return {
        "status": "UP",
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "temporal_worker": "ENABLED" if settings.TEMPORAL_WORKER_ENABLED else "DISABLED",
        "task_queue": settings.TEMPORAL_TASK_QUEUE
    }

@app.get("/")
async def root():
    return {
        "name": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "docs": "/docs",
        "health": "/healthz"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.SERVER_HOST, port=settings.SERVER_PORT, reload=settings.DEBUG)
