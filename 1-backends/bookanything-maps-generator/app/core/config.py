import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # App Settings
    APP_NAME: str = "bookanything-maps-generator"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False
    SERVER_HOST: str = "0.0.0.0"
    SERVER_PORT: int = int(os.getenv("SERVER_PORT", "8000"))

    # Temporal Settings
    TEMPORAL_HOST_PORT: str = os.getenv("TEMPORAL_HOST_PORT", os.getenv("TEMPORAL_SERVICE_ADDRESS", "temporal-frontend.drr-corpshared-plat.svc.cluster.local:7233"))
    TEMPORAL_NAMESPACE: str = os.getenv("TEMPORAL_NAMESPACE", "corporate-core")
    TEMPORAL_TASK_QUEUE: str = os.getenv("TEMPORAL_TASK_QUEUE", "MAPS_GENERATOR_TASK_QUEUE")
    TEMPORAL_WORKER_ENABLED: bool = os.getenv("TEMPORAL_WORKER_ENABLED", "true").lower() in ("true", "1", "yes")

    # MinIO Settings
    MINIO_ENDPOINT: str = os.getenv("MINIO_ENDPOINT", os.getenv("MINIO_URL", "http://tenant-minio.drr-tnt-swfabrik-europe-dev.svc.cluster.local:9000"))
    MINIO_ACCESS_KEY: str = os.getenv("MINIO_ACCESS_KEY", "admin@dexterity.org.de")
    MINIO_SECRET_KEY: str = os.getenv("MINIO_SECRET_KEY", "admin_1a88a1")
    MINIO_BUCKET_IMAGES: str = os.getenv("MINIO_BUCKET_IMAGES", "bookanything-images")
    MINIO_SECURE: bool = os.getenv("MINIO_SECURE", "false").lower() in ("true", "1", "yes")

    class Config:
        env_file = ".env"
        extra = "allow"

settings = Settings()
