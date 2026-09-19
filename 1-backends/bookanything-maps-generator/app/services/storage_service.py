import io
import logging
from typing import Tuple
from minio import Minio
from minio.error import S3Error

from app.core.config import settings

logger = logging.getLogger("storage_service")

class StorageService:
    def __init__(self):
        # Extract host:port from MINIO_ENDPOINT if it contains http:// or https://
        endpoint = settings.MINIO_ENDPOINT.replace("http://", "").replace("https://", "").rstrip("/")
        self.client = Minio(
            endpoint=endpoint,
            access_key=settings.MINIO_ACCESS_KEY,
            secret_key=settings.MINIO_SECRET_KEY,
            secure=settings.MINIO_SECURE
        )
        self.default_bucket = settings.MINIO_BUCKET_IMAGES

    def ensure_bucket_exists(self, bucket_name: str):
        try:
            if not self.client.bucket_exists(bucket_name):
                logger.info("Bucket '%s' does not exist. Creating it...", bucket_name)
                self.client.make_bucket(bucket_name)
        except Exception as e:
            logger.error("Error checking or creating bucket '%s': %s", bucket_name, e)
            raise

    def upload_svg_maps(
        self,
        geo_location_id: int,
        friendly_id: str,
        local_map_svg: str,
        world_highlight_svg: str,
        bucket_override: str = None
    ) -> Tuple[str, str, str, str]:
        """
        Uploads both Local Map and World Map SVGs to Tenant MinIO following the standardized key layout:
        images/geolocations/{geo_location_id}/map-{friendly_id}.svg
        images/geolocations/{geo_location_id}/world-{friendly_id}.svg

        Returns: (local_storage_key, world_storage_key, local_url, world_url)
        """
        bucket = bucket_override or self.default_bucket
        self.ensure_bucket_exists(bucket)

        local_key = f"images/geolocations/{geo_location_id}/map-{friendly_id}.svg"
        world_key = f"images/geolocations/{geo_location_id}/world-{friendly_id}.svg"

        # 1. Upload Local Map SVG
        local_bytes = local_map_svg.encode('utf-8')
        with io.BytesIO(local_bytes) as data:
            self.client.put_object(
                bucket_name=bucket,
                object_name=local_key,
                data=data,
                length=len(local_bytes),
                content_type="image/svg+xml"
            )
        logger.info("Uploaded Local Map SVG to s3://%s/%s (%d bytes)", bucket, local_key, len(local_bytes))

        # 2. Upload World Highlight SVG
        world_bytes = world_highlight_svg.encode('utf-8')
        with io.BytesIO(world_bytes) as data:
            self.client.put_object(
                bucket_name=bucket,
                object_name=world_key,
                data=data,
                length=len(world_bytes),
                content_type="image/svg+xml"
            )
        logger.info("Uploaded World Highlight SVG to s3://%s/%s (%d bytes)", bucket, world_key, len(world_bytes))

        local_url = f"s3://{bucket}/{local_key}"
        world_url = f"s3://{bucket}/{world_key}"

        return local_key, world_key, local_url, world_url
