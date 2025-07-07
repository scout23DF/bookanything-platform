package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.input.kafka

import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.CentroDistribuicaoOrchestrationService
import de.org.dexterity.bookanything.dom02distributioncenterlocator.application.GeoJsonProcessingService
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.geojson.GeoJsonUploadedFileDTO
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
