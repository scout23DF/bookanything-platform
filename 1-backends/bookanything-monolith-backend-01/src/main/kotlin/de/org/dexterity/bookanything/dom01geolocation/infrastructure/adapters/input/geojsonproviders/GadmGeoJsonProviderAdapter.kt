package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.geojson_providers

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.GeoJsonProviderPort
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path

@Component
class GadmGeoJsonProviderAdapter(
    private val webClient: WebClient,
    @Value("\${gadm.api.base-url}") private val baseUrl: String,
    @Value("\${gadm.api.file-template}") private val fileTemplate: String
) : GeoJsonProviderPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun getFileUrl(countryIso3Code: String, level: Int): String {
        val fileName = fileTemplate
            .replace("[COUNTRY_ALIAS]", countryIso3Code)
            .replace("[LEVEL_NUMBER]", level.toString())
        return "$baseUrl/$fileName"
    }

    override suspend fun fileExists(countryIso3Code: String, level: Int): Boolean {
        val url = getFileUrl(countryIso3Code, level)
        return try {
            val response = webClient.head().uri(url).retrieve().toBodilessEntity().awaitSingle()
            response.statusCode.is2xxSuccessful
        } catch (e: Exception) {
            logger.warn("HEAD request for {} failed: {}", url, e.message)
            false
        }
    }

    override suspend fun downloadFile(countryIso3Code: String, level: Int): Path {
        val url = getFileUrl(countryIso3Code, level)
        logger.info("Downloading GeoJSON from: {}", url)

        val tempFile = Files.createTempFile("geojson_${countryIso3Code}_${level}_", ".json")

        val bodyFlux = webClient.get()
            .uri(url)
            .retrieve()
            .onStatus({ it == HttpStatus.NOT_FOUND }) {
                Mono.error(FileNotFoundException("GADM file not found at $url"))
            }
            .bodyToFlux<DataBuffer>()

        return DataBufferUtils.write(bodyFlux, tempFile)
            .then(Mono.just(tempFile))
            .awaitSingle()
    }

    fun getFileName(countryIso3Code: String, level: Int): String {
        return fileTemplate
            .replace("[COUNTRY_ALIAS]", countryIso3Code)
            .replace("[LEVEL_NUMBER]", level.toString())
    }
}