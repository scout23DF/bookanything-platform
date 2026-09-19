import os
import io
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patheffects as pe
from matplotlib.patches import FancyBboxPatch
import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon
from shapely import wkt
import pyproj

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORLD_GEOJSON = os.path.join(BASE_DIR, 'app', 'assets', 'naturalearth', 'world_countries.geojson')

def generate_local_map_svg(geom_wkt: str, name: str, friendly_id: str, geo_type: str = "PROVINCE") -> str:
    """
    Generates a stunning, cartographically rich SVG map of the local GeoLocation.
    """
    geom = wkt.loads(geom_wkt)
    gdf_local = gpd.GeoDataFrame([{'name': name, 'geometry': geom}], crs="EPSG:4326")
    
    # Reproject to Web Mercator (EPSG:3857) for undistorted conformal shape
    gdf_proj = gdf_local.to_crs(epsg=3857)
    proj_geom = gdf_proj.geometry.iloc[0]
    
    fig, ax = plt.subplots(figsize=(8, 6), dpi=100)
    fig.patch.set_facecolor('#0b1120')
    ax.set_facecolor('#0b1120')
    
    # 1. Subtle grid lines
    ax.grid(True, linestyle='--', alpha=0.18, color='#38bdf8', linewidth=0.6)
    
    # 2. Outer glow / buffer
    try:
        bounds = proj_geom.bounds
        extent_w = bounds[2] - bounds[0]
        extent_h = bounds[3] - bounds[1]
        buffer_dist = max(extent_w, extent_h) * 0.03
        halo_geom = proj_geom.buffer(buffer_dist)
        gpd.GeoSeries([halo_geom], crs=gdf_proj.crs).plot(
            ax=ax,
            facecolor='#0284c7',
            alpha=0.12,
            edgecolor='#0284c7',
            linewidth=1.5
        )
    except Exception:
        pass

    # 3. Main geometry plot
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
    
    # Hide axis ticks for minimalist modern cartography
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
    
    # 7. Header title box
    ax.text(
        0.04, 0.93,
        f"{name.upper()}",
        transform=ax.transAxes,
        fontsize=14,
        fontweight='bold',
        color='#f8fafc',
        ha='left',
        va='top',
        path_effects=[pe.withStroke(linewidth=3, foreground='#0b1120')]
    )
    ax.text(
        0.04, 0.86,
        f"{geo_type} • ID: {friendly_id}",
        transform=ax.transAxes,
        fontsize=9,
        fontweight='medium',
        color='#94a3b8',
        ha='left',
        va='top'
    )
    
    # 8. Modern Footer Tag
    ax.text(
        0.04, 0.04,
        "GeoPandas High-Fidelity Cartography • WGS84/EPSG:3857",
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
    
    raw_svg = svg_buf.getvalue()
    # Ensure viewBox and width/height 100%
    return raw_svg


def generate_world_highlight_map_svg(geom_wkt: str, name: str, friendly_id: str) -> str:
    """
    Generates a stunning World Map SVG highlighting the target GeoLocation in neon cyan on Equal Earth projection.
    """
    world_gdf = gpd.read_file(WORLD_GEOJSON)
    # Use Equal Earth projection (+proj=eqearth)
    proj_crs = "+proj=eqearth"
    world_proj = world_gdf.to_crs(proj_crs)
    
    target_geom = wkt.loads(geom_wkt)
    target_gdf = gpd.GeoDataFrame([{'name': name, 'geometry': target_geom}], crs="EPSG:4326").to_crs(proj_crs)
    
    fig, ax = plt.subplots(figsize=(10, 5), dpi=100)
    fig.patch.set_facecolor('#090d16')
    ax.set_facecolor('#090d16')
    
    # 1. Subtle graticules / background frame
    ax.grid(True, linestyle=':', alpha=0.15, color='#38bdf8', linewidth=0.5)
    
    # 2. Base World Countries
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
    
    # Concentric pulse ring around centroid
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
    bbox = ax.get_position()
    ax.annotate(
        f"{name} ({friendly_id})",
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
    
    return svg_buf.getvalue()

if __name__ == '__main__':
    # Test with sample polygon for Brazil
    sample_wkt = "MULTIPOLYGON (((-53.3788 -33.7505, -53.6505 -33.7259, -53.4986 -33.6268, -48.4371 -27.8488, -44.2044 -23.2783, -34.8028 -7.1158, -35.2104 -5.1636, -44.3013 -2.2575, -50.9328 2.5204, -60.0381 5.2638, -69.8829 -4.2417, -73.9831 -7.5348, -70.0898 -13.0133, -57.6251 -22.1852, -53.6496 -27.8687, -53.3788 -33.7505)))"
    
    print("Generating local map SVG...")
    local_svg = generate_local_map_svg(sample_wkt, "Brazil", "BRA", "COUNTRY")
    print("Local SVG generated! Length:", len(local_svg))
    with open("test_local_map.svg", "w") as f:
        f.write(local_svg)
        
    print("Generating world highlight SVG...")
    world_svg = generate_world_highlight_map_svg(sample_wkt, "Brazil", "BRA")
    print("World SVG generated! Length:", len(world_svg))
    with open("test_world_map.svg", "w") as f:
        f.write(world_svg)
        
    print("SUCCESS!")
