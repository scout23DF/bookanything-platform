import logging
from fastapi import APIRouter, HTTPException, Response
from app.models.schemas import GeoLocationMapsRequest, GeoLocationMapsResponse
from app.services.cartography_service import CartographyService
from app.services.storage_service import StorageService

logger = logging.getLogger("maps_router")
router = APIRouter(prefix="/api/v1/maps", tags=["maps"])

storage_service = StorageService()

@router.post("/generate", response_model=GeoLocationMapsResponse)
async def generate_maps(request: GeoLocationMapsRequest):
    """
    Generates high-fidelity Local Map and World Highlight SVGs using GeoPandas and Matplotlib.
    Optionally uploads to Tenant MinIO.
    """
    try:
        local_svg = CartographyService.generate_local_map_svg(request)
        world_svg = CartographyService.generate_world_highlight_map_svg(request)

        local_key = None
        world_key = None
        local_url = None
        world_url = None

        if request.save_to_minio:
            local_key, world_key, local_url, world_url = storage_service.upload_svg_maps(
                geo_location_id=request.geo_location_id,
                friendly_id=request.friendly_id,
                local_map_svg=local_svg,
                world_highlight_svg=world_svg,
                bucket_override=request.minio_bucket
            )

        return GeoLocationMapsResponse(
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
    except Exception as e:
        logger.error("Error generating maps: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/preview/local")
async def preview_local_map(request: GeoLocationMapsRequest):
    """
    Returns the local SVG map directly with Content-Type: image/svg+xml for easy in-browser preview.
    """
    try:
        svg = CartographyService.generate_local_map_svg(request)
        return Response(content=svg, media_type="image/svg+xml")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/preview/world")
async def preview_world_map(request: GeoLocationMapsRequest):
    """
    Returns the world highlight SVG map directly with Content-Type: image/svg+xml for easy in-browser preview.
    """
    try:
        svg = CartographyService.generate_world_highlight_map_svg(request)
        return Response(content=svg, media_type="image/svg+xml")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
