package de.org.dexterity.bookanything.dom01geolocation.application.services

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Polygon
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class GeoLocationSvgGeneratorService {

    /**
     * Generates a modern, vectorized SVG representation of the GeoLocation boundary.
     * Uses the geometry's bounding box to center and scale the MultiPolygon/Polygon.
     */
    fun generateLocalMapSvg(geometry: Geometry, title: String, alias: String? = null): String {
        val env = geometry.envelopeInternal
        val minX = env.minX
        val maxX = env.maxX
        val minY = env.minY
        val maxY = env.maxY
        val spanX = (maxX - minX).coerceAtLeast(0.0001)
        val spanY = (maxY - minY).coerceAtLeast(0.0001)

        val svgWidth = 800.0
        val svgHeight = 600.0
        val padding = 60.0

        val availW = svgWidth - 2 * padding
        val availH = svgHeight - 2 * padding

        val scale = minOf(availW / spanX, availH / spanY)
        val offsetX = padding + (availW - spanX * scale) / 2.0
        val offsetY = padding + (availH - spanY * scale) / 2.0

        val project = { lon: Double, lat: Double ->
            val px = offsetX + (lon - minX) * scale
            val py = offsetY + (maxY - lat) * scale
            Pair(px, py)
        }

        val pathData = buildSvgPath(geometry, project)

        val displayTitle = escapeXml(title)
        val displayAlias = escapeXml(alias ?: "")
        val bboxLabel = String.format(Locale.US, "[%.3f, %.3f] to [%.3f, %.3f]", minX, minY, maxX, maxY)

        return """
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
          <defs>
            <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#0b0f19" />
              <stop offset="100%" stop-color="#1e293b" />
            </linearGradient>
            <linearGradient id="polyGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#38bdf8" />
              <stop offset="50%" stop-color="#2563eb" />
              <stop offset="100%" stop-color="#1d4ed8" />
            </linearGradient>
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="4" stdDeviation="8" flood-color="#0284c7" flood-opacity="0.4" />
            </filter>
            <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#334155" stroke-width="0.5" opacity="0.3"/>
            </pattern>
          </defs>

          <!-- Background -->
          <rect width="800" height="600" fill="url(#bgGrad)" rx="8" />
          <rect width="800" height="600" fill="url(#grid)" rx="8" />

          <!-- Title Bar -->
          <g transform="translate(24, 36)">
            <text fill="#f8fafc" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="18" font-weight="700">$displayTitle</text>
            <text y="18" fill="#94a3b8" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="11">$displayAlias &bull; BBox: $bboxLabel</text>
          </g>

          <!-- North Compass Indicator -->
          <g transform="translate(750, 45)">
            <circle r="16" fill="#1e293b" stroke="#475569" stroke-width="1"/>
            <polygon points="0,-12 4,2 -4,2" fill="#ef4444"/>
            <polygon points="0,12 4,2 -4,2" fill="#94a3b8"/>
            <text y="-14" text-anchor="middle" fill="#ef4444" font-size="8" font-weight="bold" font-family="sans-serif">N</text>
          </g>

          <!-- Vector Boundary Path -->
          <path d="$pathData"
                fill="url(#polyGrad)"
                fill-opacity="0.85"
                stroke="#7dd3fc"
                stroke-width="2"
                stroke-linejoin="round"
                fill-rule="evenodd"
                filter="url(#glow)"/>

          <!-- Footer Watermark -->
          <text x="24" y="580" fill="#64748b" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="10">
            Darueira Geospatial Intelligence &bull; PostGIS Vector Geometry Engine
          </text>
        </svg>
        """.trimIndent()
    }

    /**
     * Generates a standard World Map SVG with continents in slate and the target GeoLocation
     * highlighted in bright contrasting red/coral with a radar beacon at its centroid.
     */
    fun generateWorldHighlightMapSvg(geometry: Geometry, title: String, alias: String? = null): String {
        // World Equirectangular projection: lon -180..180 -> x: 0..1000, lat -90..90 -> y: 500..0
        val projectWorld = { lon: Double, lat: Double ->
            val px = (lon + 180.0) / 360.0 * 1000.0
            val py = (90.0 - lat) / 180.0 * 500.0
            Pair(px, py)
        }

        val targetPathData = buildSvgPath(geometry, projectWorld)

        val centroid = geometry.centroid
        val (cx, cy) = projectWorld(centroid.x, centroid.y)
        val formattedCx = String.format(Locale.US, "%.2f", cx)
        val formattedCy = String.format(Locale.US, "%.2f", cy)

        val displayTitle = escapeXml(title)
        val displayAlias = escapeXml(alias ?: "")

        return """
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 500" width="100%" height="100%">
          <defs>
            <linearGradient id="oceanGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#090d16"/>
              <stop offset="100%" stop-color="#0f172a"/>
            </linearGradient>
            <filter id="radarGlow" x="-50%" y="-50%" width="200%" height="200%">
              <feDropShadow dx="0" dy="0" stdDeviation="6" flood-color="#ef4444" flood-opacity="0.8"/>
            </filter>
          </defs>

          <!-- Ocean Background -->
          <rect width="1000" height="500" fill="url(#oceanGrad)" rx="8"/>

          <!-- Latitude / Longitude Graticule -->
          <g stroke="#334155" stroke-width="0.5" opacity="0.25">
            <!-- Longitude Lines -->
            <line x1="250" y1="0" x2="250" y2="500"/>
            <line x1="500" y1="0" x2="500" y2="500"/>
            <line x1="750" y1="0" x2="750" y2="500"/>
            <!-- Equator & Tropic Lines -->
            <line x1="0" y1="250" x2="1000" y2="250" stroke="#475569" stroke-dasharray="4,4"/>
            <line x1="0" y1="185" x2="1000" y2="185"/>
            <line x1="0" y1="315" x2="1000" y2="315"/>
          </g>

          <!-- Continents Outlines (Simplified Natural Earth Landmass) -->
          <g fill="#1e293b" stroke="#334155" stroke-width="1" opacity="0.95">
            <!-- North America -->
            <path d="M 70,60 L 160,50 L 240,70 L 300,90 L 260,140 L 280,200 L 210,230 L 180,270 L 160,250 L 130,220 L 100,160 L 60,110 Z"/>
            <!-- South America -->
            <path d="M 230,270 L 330,270 L 370,320 L 350,380 L 320,440 L 290,470 L 280,440 L 260,370 L 220,310 Z"/>
            <!-- Eurasia (Europe + Asia) -->
            <path d="M 450,80 L 520,60 L 600,60 L 720,70 L 880,90 L 920,130 L 870,180 L 800,200 L 750,260 L 680,240 L 630,170 L 560,160 L 500,190 L 460,150 L 440,110 Z"/>
            <!-- Africa -->
            <path d="M 470,180 L 580,180 L 620,240 L 590,320 L 550,400 L 510,410 L 460,340 L 440,260 L 440,200 Z"/>
            <!-- Australia & Oceania -->
            <path d="M 780,310 L 880,300 L 910,360 L 870,410 L 800,400 L 760,360 Z"/>
            <!-- Antarctica -->
            <path d="M 150,470 L 350,480 L 600,480 L 850,470 L 950,490 L 50,490 Z"/>
          </g>

          <!-- Target GeoLocation Polygon Vector Overlay -->
          <path d="$targetPathData"
                fill="#ef4444"
                fill-opacity="0.9"
                stroke="#fca5a5"
                stroke-width="2.5"
                stroke-linejoin="round"
                fill-rule="evenodd"
                filter="url(#radarGlow)"/>

          <!-- Radar Target Crosshairs at Centroid -->
          <g transform="translate($formattedCx, $formattedCy)" filter="url(#radarGlow)">
            <circle r="14" fill="none" stroke="#ef4444" stroke-width="1.5" stroke-dasharray="3,3" opacity="0.8"/>
            <circle r="7" fill="none" stroke="#ef4444" stroke-width="2"/>
            <circle r="2.5" fill="#ffffff"/>
            <line x1="-18" y1="0" x2="-8" y2="0" stroke="#ef4444" stroke-width="1.5"/>
            <line x1="8" y1="0" x2="18" y2="0" stroke="#ef4444" stroke-width="1.5"/>
            <line x1="0" y1="-18" x2="0" y2="-8" stroke="#ef4444" stroke-width="1.5"/>
            <line x1="0" y1="8" x2="0" y2="18" stroke="#ef4444" stroke-width="1.5"/>
          </g>

          <!-- Location Badge Overlay -->
          <g transform="translate(20, 30)">
            <rect width="240" height="48" fill="#1e293b" opacity="0.9" rx="6" stroke="#475569" stroke-width="1"/>
            <text x="12" y="20" fill="#f8fafc" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="13" font-weight="700">$displayTitle</text>
            <text x="12" y="36" fill="#f87171" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="10" font-weight="600">TARGET: $displayAlias &bull; [$formattedCx, $formattedCy]</text>
          </g>
        </svg>
        """.trimIndent()
    }

    private fun buildSvgPath(geom: Geometry, projectCoord: (Double, Double) -> Pair<Double, Double>): String {
        val sb = StringBuilder()
        for (i in 0 until geom.numGeometries) {
            val subGeom = geom.getGeometryN(i)
            if (subGeom is Polygon) {
                appendPolygon(subGeom, projectCoord, sb)
            }
        }
        return sb.toString().trim()
    }

    private fun appendPolygon(poly: Polygon, projectCoord: (Double, Double) -> Pair<Double, Double>, sb: StringBuilder) {
        appendRing(poly.exteriorRing.coordinates, projectCoord, sb)
        for (j in 0 until poly.numInteriorRing) {
            appendRing(poly.getInteriorRingN(j).coordinates, projectCoord, sb)
        }
    }

    private fun appendRing(coords: Array<Coordinate>, projectCoord: (Double, Double) -> Pair<Double, Double>, sb: StringBuilder) {
        if (coords.isEmpty()) return
        for (k in coords.indices) {
            val (x, y) = projectCoord(coords[k].x, coords[k].y)
            val fx = String.format(Locale.US, "%.2f", x)
            val fy = String.format(Locale.US, "%.2f", y)
            if (k == 0) {
                sb.append("M ").append(fx).append(" ").append(fy).append(" ")
            } else {
                sb.append("L ").append(fx).append(" ").append(fy).append(" ")
            }
        }
        sb.append("Z ")
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
