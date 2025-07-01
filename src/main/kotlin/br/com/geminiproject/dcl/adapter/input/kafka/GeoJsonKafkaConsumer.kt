package br.com.geminiproject.dcl.adapter.input.kafka

import br.com.geminiproject.dcl.application.CentroDistribuicaoOrchestrationService
import br.com.geminiproject.dcl.application.GeoJsonProcessingService
import br.com.geminiproject.dcl.domain.geojson.GeoJsonUploadedFileDTO
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class GeoJsonKafkaConsumer(
    private val geoJsonProcessingService: GeoJsonProcessingService,
    private val centroDistribuicaoOrchestrationService: CentroDistribuicaoOrchestrationService
) {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @KafkaListener(topics = ["geojson-upload-topic"], groupId = "geojson-processor")
    fun listen(geoJsonUploadedFileDTO: GeoJsonUploadedFileDTO) {

        geoJsonProcessingService.handleUploadedGeoJsonFileConsumedFromQueue(geoJsonUploadedFileDTO)

    }

}
