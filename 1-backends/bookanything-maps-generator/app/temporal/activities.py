import logging
from temporalio import activity
from app.models.schemas import GeoLocationMapsRequest, GeoLocationMapsResponse
from app.services.cartography_service import CartographyService
from app.services.storage_service import StorageService

logger = logging.getLogger("temporal_activities")
storage_service = StorageService()

@activity.defn(name="GenerateGeoLocationMaps")
async def generate_geo_location_maps_activity(request_input: dict) -> dict:
    """
    Temporal Polyglot Activity to generate GeoLocation Local Map and World Highlight SVGs
    using GeoPandas, Shapely, and Matplotlib.
    Persists them to Tenant MinIO and returns the SVG contents and storage keys.
    """
    logger.info("Executing Temporal Activity 'GenerateGeoLocationMaps' with input: %s", request_input)
    try:
        # Convert dict to Pydantic model (supports camelCase and snake_case)
        if isinstance(request_input, dict):
            request = GeoLocationMapsRequest(**request_input)
        elif isinstance(request_input, GeoLocationMapsRequest):
            request = request_input
        else:
            raise ValueError(f"Unsupported request type: {type(request_input)}")

        logger.info(
            "Generating high-fidelity maps for GeoLocation #%d (%s, %s)...",
            request.geo_location_id, request.name, request.friendly_id
        )

        # 1. Generate SVGs
        local_svg = CartographyService.generate_local_map_svg(request)
        world_svg = CartographyService.generate_world_highlight_map_svg(request)

        local_key = None
        world_key = None
        local_url = None
        world_url = None

        # 2. Upload to MinIO if enabled
        if request.save_to_minio:
            logger.info("Uploading generated SVGs to Tenant MinIO for #%d...", request.geo_location_id)
            local_key, world_key, local_url, world_url = storage_service.upload_svg_maps(
                geo_location_id=request.geo_location_id,
                friendly_id=request.friendly_id,
                local_map_svg=local_svg,
                world_highlight_svg=world_svg,
                bucket_override=request.minio_bucket
            )

        response = GeoLocationMapsResponse(
            geo_location_id=request.geo_location_id,
            friendly_id=request.friendly_id,
            local_map_svg=local_svg,
            world_highlight_svg=world_svg,
            local_map_storage_key=local_key,
            world_highlight_storage_key=world_key,
            local_map_url=local_url,
            world_highlight_url=world_url,
            status="SUCCESS"
        )
        return response.model_dump(by_alias=True)

    except Exception as e:
        logger.error("Error in Temporal Activity 'GenerateGeoLocationMaps': %s", e, exc_info=True)
        return {
            "geoLocationId": request_input.get("geoLocationId") or request_input.get("geo_location_id", 0),
            "friendlyId": request_input.get("friendlyId") or request_input.get("friendly_id", ""),
            "localMapSvg": "",
            "worldHighlightSvg": "",
            "status": "ERROR",
            "errorMessage": str(e)
        }
