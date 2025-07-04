package br.com.geminiproject.dcl.application

import br.com.geminiproject.dcl.domain.geojson.GeoJsonUploadedFileDTO
import com.bedatadriven.jackson.datatype.jts.JtsModule
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.geojson.FeatureCollection
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

// Using RuntimeException is common in Spring for exceptions that should result in a client error.
class InvalidGeoJsonFileException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


class GeoJsonProcessingService(
    private val kafkaTemplate: KafkaTemplate<String, GeoJsonUploadedFileDTO>,
    private val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) {


    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper().registerModule(JtsModule())
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun processGeoJsonFile(contentDataType: String, uploadedGeoJSONFile: MultipartFile) {

        try {

            val geoJsonUploadedFileDTO = mountGeoJSONObjectFromUploadedFile(contentDataType, uploadedGeoJSONFile)
            this.kafkaTemplate.send("geojson-upload-topic", geoJsonUploadedFileDTO)
            logger.info("Successfully processed and sent GeoJSON file to Kafka topic.")
        } catch (e: InvalidGeoJsonFileException) {
            logger.error("Failed to process GeoJSON file: ${e.message}", e)
            // Re-throw the exception so the controller layer can handle it and return a proper HTTP status.
            throw e
        }

    }

    private fun mountGeoJSONObjectFromUploadedFile(contentDataType: String, uploadedGeoJSONFile: MultipartFile) : GeoJsonUploadedFileDTO? {

        try {

            val geoJsonNode = objectMapper.readTree(uploadedGeoJSONFile.inputStream)
            val featureCollection = objectMapper.treeToValue(geoJsonNode, FeatureCollection::class.java)

            return GeoJsonUploadedFileDTO(
                contentDataType,
                geoJsonNode.get("type").asText(),
                featureCollection
            )

        } catch (e: JsonMappingException) {
            logger.error("Error deserializing the uploaded GeoJSON file: Invalid JSON format or structure.", e)
            throw InvalidGeoJsonFileException("The provided file is not a valid GeoJSON: ${e.localizedMessage}", e)
        } catch (e: JsonProcessingException) {
            logger.error("Error deserializing the uploaded GeoJSON file: Invalid JSON format or structure.", e)
            throw InvalidGeoJsonFileException("The provided file is not a valid GeoJSON: ${e.localizedMessage}", e)
        } catch (e: IOException) {
            logger.error("Error reading the uploaded GeoJSON file.", e)
            throw InvalidGeoJsonFileException("Could not read the provided file.", e)
        } catch (e: Exception) {
            logger.error("Error reading the uploaded GeoJSON file.", e)
            throw InvalidGeoJsonFileException("Could not read the provided file.", e)
        }
    }


    fun handleUploadedGeoJsonFileConsumedFromQueue(geoJsonUploadedFileDTO: GeoJsonUploadedFileDTO) {
        geoJsonUploadedFileDTO.featureCollection.features.forEach { oneFeature ->
            val nome: String = buildNomeFromProperties(oneFeature.properties)
            if (oneFeature.geometry is org.geojson.Point) {
                val geoJsonPoint = oneFeature.geometry as org.geojson.Point
                val coordinates = geoJsonPoint.coordinates
                if (coordinates != null) {
                    val locationCoords: Point = geometryFactory.createPoint(
                        Coordinate(
                            coordinates.longitude,
                            coordinates.latitude
                        )
                    )

                    try {
                        centroDistribuicaoOrchestrationService.cadastrar(nome, locationCoords)
                        logger.info("==> SUCCESS :: O seguinte Centro de Distribuição foi cadastrado com sucesso: {} - Coordenadas: {}", nome, locationCoords)
                    } catch (ex: IllegalArgumentException) {
                        logger.warn("==> FAIL :: Verificação de Duplicação de Centro de Distribuição: {}", ex.message)
                    }
                } else {
                    logger.warn("Feature with name '{}' has a Point geometry with null coordinates.", nome)
                }
            }
        }
    }

    /*
    fun handleUploadedGeoJsonFileConsumedFromQueue(geoJsonUploadedFileDTO: GeoJsonUploadedFileDTO) {

        geoJsonUploadedFileDTO.featureCollection.features.forEach { oneFeature ->
            val nome: String = buildNomeFromProperties(oneFeature.properties)
            if (oneFeature.geometry is org.geojson.Point) {
                val locationCoords: Point = geometryFactory.createPoint(
                    Coordinate(
                        (oneFeature.geometry as org.geojson.Point).coordinates.longitude,
                        (oneFeature.geometry as org.geojson.Point).coordinates.latitude
                    )
                )

                try {
                    centroDistribuicaoOrchestrationService.cadastrar(nome, locationCoords)
                    println("==> SUCCESS :: O seguinte Centro de Distribuição foi cadastrado com sucesso: ${nome} - Coordenadas: ${locationCoords}")
                } catch (ex: IllegalArgumentException) {
                    println("==> FAIL :: Verificação de Duplicação de Centro de Distribuição: ${ex.message}")
                }
            }
        }

    }
    */

    private fun buildNomeFromProperties(featurePropertiesMap: Map<String, Any>): String {
        val state = featurePropertiesMap["state_province"] ?: "Unknown State"
        val city = featurePropertiesMap["city"] ?: "Unknown City"
        val station = featurePropertiesMap["station_name"] ?: "Unknown Station"
        return "$state - $city - $station"
    }

}

