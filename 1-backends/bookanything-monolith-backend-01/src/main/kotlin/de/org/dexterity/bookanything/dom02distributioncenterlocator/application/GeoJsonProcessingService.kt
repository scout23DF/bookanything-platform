package de.org.dexterity.bookanything.dom02distributioncenterlocator.application

import com.bedatadriven.jackson.datatype.jts.JtsModule
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.dtos.GeoJsonUploadedFileDTO
import org.geojson.FeatureCollection
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.*

// Using RuntimeException is common in Spring for exceptions that should result in a client error.
class InvalidGeoJsonFileException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


class GeoJsonProcessingService(
    private val kafkaTemplate: KafkaTemplate<String, GeoJsonUploadedFileDTO>,
    private val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) {


    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper().registerModule(JtsModule())
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun processGeoJsonFile(contentDataType: String, uploadedGeoJSONFile: MultipartFile): Map<String, String> {

        var resultMap : Map<String, String> = mutableMapOf()
        var messageResult : String = "The uploaded file was successfully validated and queued to be processed ASAP. You'll be notified when it gets done."

        try {

            val geoJsonUploadedFileDTO = mountGeoJSONObjectFromUploadedFile(contentDataType, uploadedGeoJSONFile)
            this.kafkaTemplate.send("geojson-upload-topic", geoJsonUploadedFileDTO)
            logger.info(messageResult)

        } catch (e: InvalidGeoJsonFileException) {
            messageResult = "Failed to process GeoJSON file: ${e.message}"
            logger.error(messageResult, e)
        }

        resultMap = mapOf("result1" to messageResult)

        return resultMap

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
            val nome: String = buildNomeFromProperties(geoJsonUploadedFileDTO.contentDataType, oneFeature.properties)
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

    private fun buildNomeFromProperties(contentDataType: String, featurePropertiesMap: Map<String, Any>): String {

        var finalBuiltName : String = ""
        var firstStmt : String
        var secondStmt : String
        var thirdStmt : String

        finalBuiltName = when (contentDataType) {
            "ev-charging-stations" -> {
                firstStmt = (featurePropertiesMap["state_province"] ?: "Unknown State") as String
                secondStmt = (featurePropertiesMap["city"] ?: "Unknown City") as String
                thirdStmt = (featurePropertiesMap["station_name"] ?: "Unknown Station") as String
                "$firstStmt - $secondStmt - $thirdStmt"
            }
            "schools" -> {
                firstStmt = (featurePropertiesMap["Phase"] ?: "Unknown Phase") as String
                secondStmt = (featurePropertiesMap["Establishment"] ?: "Unknown Establishment") as String
                thirdStmt = (featurePropertiesMap["School ward"] ?: "Unknown School ward") as String
                "$firstStmt - $secondStmt - $thirdStmt"
            }
            "e-scooters" -> {
                firstStmt = (featurePropertiesMap["OBJECTID"]?.toString() ?: "Unknown OBJECTID") as String
                secondStmt = (featurePropertiesMap["Location"] ?: "Unknown Location") as String
                thirdStmt = (featurePropertiesMap["WARD"] ?: "Unknown WARD") as String
                "$firstStmt - $secondStmt - $thirdStmt"
            }
            "grocery-stores" -> {
                firstStmt = (featurePropertiesMap["State"] ?: "Unknown State") as String
                secondStmt = (featurePropertiesMap["City"] ?: "Unknown City") as String
                thirdStmt = (featurePropertiesMap["Company"] ?: "Unknown Company") as String
                "$firstStmt - $secondStmt - $thirdStmt"
            }
            "museums" -> {
                firstStmt = (featurePropertiesMap["Bankfield Museum"] ?: "Unknown Bankfield Museum") as String
                secondStmt = (featurePropertiesMap["Easting"] ?: "Unknown Easting") as String
                thirdStmt = (featurePropertiesMap["Northing"] ?: "Unknown Northing") as String
                "$firstStmt - $secondStmt - $thirdStmt"
            }
            "pharmacies" -> {
                firstStmt = (featurePropertiesMap["East"] ?: "Unknown East") as String
                secondStmt = (featurePropertiesMap["PharmacyName"] ?: "Unknown PharmacyName") as String
                thirdStmt = (featurePropertiesMap["Ward"] ?: "Unknown Ward") as String
                "$firstStmt - $secondStmt - $thirdStmt"
            }
            "theatres" -> {
                firstStmt = (featurePropertiesMap["NAME"] ?: "Unknown NAME") as String
                secondStmt = (featurePropertiesMap["Northings"] ?: "Unknown Northings") as String
                thirdStmt = (featurePropertiesMap["Eastings"] ?: "Unknown Eastings") as String
                "$firstStmt - $secondStmt - $thirdStmt"
            }
            "dentists" -> {
                firstStmt = (featurePropertiesMap["County"] ?: "Unknown County") as String
                secondStmt = (featurePropertiesMap["City"] ?: "Unknown City") as String
                thirdStmt = (featurePropertiesMap["Name"] ?: "Unknown Name") as String
                "$firstStmt - $secondStmt - $thirdStmt"
            }
            else -> {
                "[Name Not Defined - ${UUID.randomUUID()}]"
            }
        }

        return finalBuiltName
    }

}

