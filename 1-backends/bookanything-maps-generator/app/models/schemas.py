from typing import Optional, Any
from pydantic import BaseModel, Field, ConfigDict

def to_camel(string: str) -> str:
    components = string.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

class GeoLocationMapsRequest(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        alias_generator=to_camel,
        extra="allow"
    )

    geo_location_id: int = Field(..., description="Unique ID of the GeoLocation")
    name: str = Field(..., description="Name of the GeoLocation")
    type: str = Field(default="COUNTRY", description="Type (e.g. COUNTRY, PROVINCE, CITY)")
    friendly_id: str = Field(..., description="Friendly identifier (e.g. BRA, BR-AM)")
    alias: Optional[str] = Field(default=None, description="Alias (e.g. Amazonas)")
    boundary_wkt: str = Field(..., description="WKT representation of the PostGIS geometry")
    save_to_minio: bool = Field(default=True, description="Whether to persist SVGs to MinIO")
    minio_bucket: Optional[str] = Field(default=None, description="Target MinIO bucket override")

class GeoLocationMapsResponse(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        alias_generator=to_camel,
        extra="allow"
    )

    geo_location_id: int
    friendly_id: str
    local_map_svg: str
    world_highlight_svg: str
    local_map_storage_key: Optional[str] = None
    world_highlight_storage_key: Optional[str] = None
    local_map_url: Optional[str] = None
    world_highlight_url: Optional[str] = None
    status: str = "SUCCESS"
    error_message: Optional[str] = None
