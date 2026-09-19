package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.rest

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationMapsRequestDto
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoLocationMapsResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URI

@Component
class GeoLocationPythonMapsClient(
    @Value("\${application.domain-settings.geolocation.maps-generator-url:http://bookanything-maps-generator.drr-tnt-swfabrik-europe-dev.svc.cluster.local:8000}")
    private val mapsGeneratorUrl: String,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun generateMaps(request: GeoLocationMapsRequestDto): GeoLocationMapsResponseDto {
        val targetUrl = "$mapsGeneratorUrl/api/v1/maps/generate"
        logger.info("Calling Python Maps Generator at: $targetUrl for GeoLocation #${request.geoLocationId} (${request.name})...")

        val payload = objectMapper.writeValueAsBytes(request)
        val url = URI.create(targetUrl).toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 90000

        conn.outputStream.use { it.write(payload) }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            logger.error("Python Maps Generator failed with HTTP $responseCode: $err")
            throw RuntimeException("Python Maps Generator failed: $err")
        }

        val respBytes = conn.inputStream.use { it.readBytes() }
        val response = objectMapper.readValue(respBytes, GeoLocationMapsResponseDto::class.java)
        logger.info(
            "Python Maps Generator succeeded for #${request.geoLocationId} (Local SVG: ${response.localMapSvg.length} chars, World SVG: ${response.worldHighlightSvg.length} chars)"
        )
        return response
    }
}
