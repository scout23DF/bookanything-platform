package br.com.geminiproject.dcl.adapter.input.kafka

import br.com.geminiproject.dcl.application.CentroDistribuicaoOrchestrationService
import br.com.geminiproject.dcl.domain.geojson.GeoJsonUploadedFileDTO
import com.bedatadriven.jackson.datatype.jts.JtsModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.geojson.FeatureCollection
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class GeoJsonKafkaConsumer(
    private val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) {

    private val objectMapper = ObjectMapper().registerModule(JtsModule())
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @KafkaListener(topics = ["geojson-upload-topic"], groupId = "geojson-processor")
    fun listen(geoJsonUploadedFileDTO: GeoJsonUploadedFileDTO) {

        geoJsonUploadedFileDTO.featureCollection.features.forEach { oneFeature ->
            val nome: String = buildNomeFromProperties(oneFeature.properties)
            if (oneFeature.geometry is org.geojson.Point) {
                val locationCoords: Point = geometryFactory.createPoint(
                    Coordinate(
                        (oneFeature.geometry as org.geojson.Point).coordinates.longitude,
                        (oneFeature.geometry as org.geojson.Point).coordinates.latitude
                    )
                )
                println("==> Pronto para Cadastrar o seguinte Centro de Distribuição: ${nome} - Coordenadas: ${locationCoords}")
                // centroDistribuicaoOrchestrationService.cadastrar(nome, locationCoords)
            }
        }
    }

    private fun buildNomeFromProperties(featurePropertiesMap: MutableMap<String, Any>) : String {
        return "${featurePropertiesMap["state_province"]} - ${featurePropertiesMap["city"]} - ${featurePropertiesMap["station_name"]}"
    }

}
