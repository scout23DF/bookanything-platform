import os
import io
import re
import logging
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patheffects as pe
import geopandas as gpd
from shapely import wkt
from shapely.geometry import base

from app.models.schemas import GeoLocationMapsRequest

logger = logging.getLogger("cartography_service")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORLD_GEOJSON = os.path.join(BASE_DIR, 'assets', 'naturalearth', 'world_countries.geojson')

# Cached world GeoDataFrame and pre-projected Equal Earth layer
_world_gdf = None
_world_proj_eqearth = None

def get_world_gdf() -> gpd.GeoDataFrame:
    global _world_gdf
    if _world_gdf is None:
        if os.path.exists(WORLD_GEOJSON):
            logger.info("Loading world countries GeoJSON from %s", WORLD_GEOJSON)
            _world_gdf = gpd.read_file(WORLD_GEOJSON)
        else:
            logger.warning("world_countries.geojson not found at %s", WORLD_GEOJSON)
            _world_gdf = gpd.GeoDataFrame()
    return _world_gdf

def get_world_proj_eqearth() -> gpd.GeoDataFrame:
    global _world_proj_eqearth
    if _world_proj_eqearth is None:
        gdf = get_world_gdf()
        if not gdf.empty:
            logger.info("Pre-projecting world basemap to Equal Earth (+proj=eqearth)...")
            _world_proj_eqearth = gdf.to_crs("+proj=eqearth")
        else:
            _world_proj_eqearth = gpd.GeoDataFrame()
    return _world_proj_eqearth


def clean_svg(raw_svg: str) -> str:
    """
    Normalizes the SVG tag so it scales cleanly in HTML / JSReport containers.
    """
    svg = re.sub(r'width="[^"]+"', 'width="100%"', raw_svg, count=1)
    svg = re.sub(r'height="[^"]+"', 'height="100%"', svg, count=1)
    return svg


class CartographyService:

    @staticmethod
    def generate_local_map_svg(request: GeoLocationMapsRequest) -> str:
        """
        Renders a high-fidelity local vector cartographic map for the GeoLocation.
        """
        raw_geom = wkt.loads(request.boundary_wkt)
        if raw_geom.is_empty:
            raise ValueError("Boundary geometry is empty")

        # Simplify very dense boundaries (> 4000 vertices) for cartographic rendering speed & low memory
        num_coords = len(raw_geom.geoms[0].exterior.coords) if hasattr(raw_geom, 'geoms') and len(raw_geom.geoms) > 0 else 1000
        if num_coords > 3000:
            geom = raw_geom.simplify(tolerance=0.0015, preserve_topology=True)
        else:
            geom = raw_geom

        gdf_local = gpd.GeoDataFrame([{'name': request.name, 'geometry': geom}], crs="EPSG:4326")
        gdf_proj = gdf_local.to_crs(epsg=3857)
        proj_geom = gdf_proj.geometry.iloc[0]

        fig, ax = plt.subplots(figsize=(8, 6), dpi=100)
        fig.patch.set_facecolor('#0b1120')
        ax.set_facecolor('#0b1120')

        # 1. Subtle cartographic coordinate grid
        ax.grid(True, linestyle='--', alpha=0.18, color='#38bdf8', linewidth=0.6)

        # 2. Outer glow / boundary buffer (fast on simplified envelope/boundary)
        try:
            bounds = proj_geom.bounds
            extent_w = bounds[2] - bounds[0]
            extent_h = bounds[3] - bounds[1]
            buffer_dist = max(extent_w, extent_h) * 0.025
            simplified_for_glow = proj_geom.simplify(tolerance=max(extent_w, extent_h) * 0.005, preserve_topology=False)
            halo_geom = simplified_for_glow.buffer(buffer_dist)
            gpd.GeoSeries([halo_geom], crs=gdf_proj.crs).plot(
                ax=ax,
                facecolor='#0284c7',
                alpha=0.12,
                edgecolor='#0284c7',
                linewidth=1.5
            )
        except Exception as e:
            logger.debug("Could not plot outer glow: %s", e)

        # 3. Main geometry fill and border
        gdf_proj.plot(
            ax=ax,
            facecolor='#0284c7',
            alpha=0.45,
            edgecolor='#38bdf8',
            linewidth=2.2,
            antialiased=True
        )

        # 4. Centroid marker
        centroid = proj_geom.centroid
        ax.scatter(
            [centroid.x], [centroid.y],
            color='#f59e0b',
            s=80,
            zorder=5,
            edgecolor='#ffffff',
            linewidth=1.2,
            label='Center'
        )

        # 5. Margins and Limits
        minx, miny, maxx, maxy = proj_geom.bounds
        pad_x = (maxx - minx) * 0.12 if maxx > minx else 1000
        pad_y = (maxy - miny) * 0.12 if maxy > miny else 1000
        ax.set_xlim(minx - pad_x, maxx + pad_x)
        ax.set_ylim(miny - pad_y, maxy + pad_y)

        # Clean borders
        ax.set_xticks([])
        ax.set_yticks([])
        for spine in ax.spines.values():
            spine.set_edgecolor('#1e293b')
            spine.set_linewidth(1.5)

        # 6. North Arrow in Top Right
        ax.annotate(
            '▲\nN',
            xy=(0.94, 0.90),
            xycoords='axes fraction',
            ha='center',
            va='center',
            fontsize=13,
            fontweight='bold',
            color='#38bdf8',
            bbox=dict(boxstyle='round,pad=0.3', facecolor='#0f172a', edgecolor='#1e293b', alpha=0.85)
        )

        # 7. Header Title Box
        title_text = request.name.upper()
        ax.text(
            0.04, 0.93,
            title_text,
            transform=ax.transAxes,
            fontsize=13,
            fontweight='bold',
            color='#f8fafc',
            ha='left',
            va='top',
            path_effects=[pe.withStroke(linewidth=3, foreground='#0b1120')]
        )
        sub_text = f"{request.type} • ID: {request.friendly_id}"
        if request.alias and request.alias != request.friendly_id:
            sub_text += f" ({request.alias})"
        ax.text(
            0.04, 0.86,
            sub_text,
            transform=ax.transAxes,
            fontsize=9,
            fontweight='medium',
            color='#94a3b8',
            ha='left',
            va='top'
        )

        # 8. Modern Footer
        ax.text(
            0.04, 0.04,
            "GeoPandas Cartographic Engine • Projection: Web Mercator (EPSG:3857)",
            transform=ax.transAxes,
            fontsize=7,
            color='#475569',
            ha='left',
            va='bottom'
        )

        plt.tight_layout()
        svg_buf = io.StringIO()
        plt.savefig(svg_buf, format='svg', bbox_inches='tight', facecolor=fig.get_facecolor(), edgecolor='none')
        plt.close(fig)

        return clean_svg(svg_buf.getvalue())

    @staticmethod
    def generate_world_highlight_map_svg(request: GeoLocationMapsRequest) -> str:
        """
        Renders a world map with continents in dark slate and the target GeoLocation in glowing neon cyan.
        """
        world_proj = get_world_proj_eqearth()
        proj_crs = "+proj=eqearth"

        raw_geom = wkt.loads(request.boundary_wkt)
        # Simplify target geometry for world-scale rendering (world scale doesn't need 26k points!)
        target_geom = raw_geom.simplify(tolerance=0.01, preserve_topology=True)
        target_gdf = gpd.GeoDataFrame([{'name': request.name, 'geometry': target_geom}], crs="EPSG:4326").to_crs(proj_crs)

        fig, ax = plt.subplots(figsize=(10, 5), dpi=100)
        fig.patch.set_facecolor('#090d16')
        ax.set_facecolor('#090d16')

        # 1. Subtle graticules
        ax.grid(True, linestyle=':', alpha=0.15, color='#38bdf8', linewidth=0.5)

        # 2. Base World Countries (pre-projected, fast!)
        if not world_proj.empty:
            world_proj.plot(
                ax=ax,
                facecolor='#1e293b',
                edgecolor='#334155',
                linewidth=0.5,
                antialiased=True
            )

        # 3. Target GeoLocation Highlight
        target_proj_geom = target_gdf.geometry.iloc[0]
        target_gdf.plot(
            ax=ax,
            facecolor='#00f2fe',
            edgecolor='#38bdf8',
            linewidth=1.8,
            alpha=0.85,
            zorder=4
        )

        # 4. Target centroid & radar locator
        centroid = target_proj_geom.centroid
        ax.scatter(
            [centroid.x], [centroid.y],
            color='#f59e0b',
            s=60,
            edgecolor='#ffffff',
            linewidth=1.2,
            zorder=6
        )

        # Concentric pulse ring
        ax.scatter(
            [centroid.x], [centroid.y],
            color='none',
            s=250,
            edgecolor='#00f2fe',
            linewidth=1.5,
            alpha=0.6,
            zorder=5
        )

        # 5. Callout Leader Line and Badge
        id_label = request.alias or request.friendly_id
        ax.annotate(
            f"{request.name} ({id_label})",
            xy=(centroid.x, centroid.y),
            xytext=(centroid.x + 1200000, centroid.y + 1200000),
            textcoords='data',
            fontsize=8,
            fontweight='bold',
            color='#f8fafc',
            arrowprops=dict(
                arrowstyle='->',
                connectionstyle='arc3,rad=0.15',
                color='#00f2fe',
                linewidth=1.2
            ),
            bbox=dict(boxstyle='round,pad=0.25', facecolor='#0f172a', edgecolor='#00f2fe', alpha=0.9),
            zorder=7
        )

        ax.set_xticks([])
        ax.set_yticks([])
        for spine in ax.spines.values():
            spine.set_edgecolor('#1e293b')
            spine.set_linewidth(1.0)

        ax.text(
            0.02, 0.04,
            "World Cartographic Projection: Equal Earth (EPSG:8857 / +proj=eqearth)",
            transform=ax.transAxes,
            fontsize=7,
            color='#475569',
            ha='left',
            va='bottom'
        )

        plt.tight_layout()
        svg_buf = io.StringIO()
        plt.savefig(svg_buf, format='svg', bbox_inches='tight', facecolor=fig.get_facecolor(), edgecolor='none')
        plt.close(fig)

        return clean_svg(svg_buf.getvalue())
