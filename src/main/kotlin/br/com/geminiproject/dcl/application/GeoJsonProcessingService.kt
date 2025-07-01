package br.com.geminiproject.dcl.application

import br.com.geminiproject.dcl.domain.geojson.GeoJsonUploadedFileDTO
import com.bedatadriven.jackson.datatype.jts.JtsModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.geojson.FeatureCollection
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@Service
class GeoJsonProcessingService(
    private val kafkaTemplate: KafkaTemplate<String, GeoJsonUploadedFileDTO>,
    private val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) {

    private val objectMapper = ObjectMapper().registerModule(JtsModule())
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun processGeoJsonFile(contentDataType: String, uploadedGeoJSONFile: MultipartFile) {

        val geoJsonUploadedFileDTO : GeoJsonUploadedFileDTO = mountGeoJSONObjectFromUploadedFile(contentDataType, uploadedGeoJSONFile)

        this.kafkaTemplate.send("geojson-upload-topic", geoJsonUploadedFileDTO)

    }

    private fun mountGeoJSONObjectFromUploadedFile(contentDataType: String, uploadedGeoJSONFile: MultipartFile) : GeoJsonUploadedFileDTO {

        try {

            val geoJsonNode = objectMapper.readTree(uploadedGeoJSONFile.inputStream)
            val featureCollection = objectMapper.treeToValue(geoJsonNode, FeatureCollection::class.java)
            val geoJsonUploadedFileDTO : GeoJsonUploadedFileDTO = GeoJsonUploadedFileDTO(
                contentDataType,
                geoJsonNode.get("type").asText(),
                featureCollection
            )

            return geoJsonUploadedFileDTO

        } catch (e: IOException) {
            throw RuntimeException("Failed to read GeoJSON file", e)
        } catch (e: Exception) {
            throw RuntimeException("Error processing GeoJSON data", e)
        }
    }

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

    private fun buildNomeFromProperties(featurePropertiesMap: MutableMap<String, Any>) : String {
        return "${featurePropertiesMap["state_province"]} - ${featurePropertiesMap["city"]} - ${featurePropertiesMap["station_name"]}"
    }

}

